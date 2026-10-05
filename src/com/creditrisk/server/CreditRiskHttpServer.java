package com.creditrisk.server;

import com.creditrisk.engine.DecisionEngine;
import com.creditrisk.engine.StressTestingEngine;
import com.creditrisk.model.*;
import com.creditrisk.repository.ApplicationRepository;
import com.creditrisk.util.JsonUtil;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.Executors;

public class CreditRiskHttpServer {

    private final int port;
    private final ApplicationRepository repository;
    private final DecisionEngine decisionEngine;
    private final StressTestingEngine stressTestingEngine;
    private HttpServer server;

    public CreditRiskHttpServer(int port, ApplicationRepository repository) {
        this.port = port;
        this.repository = repository;
        this.decisionEngine = new DecisionEngine(repository.getRules());
        this.stressTestingEngine = new StressTestingEngine(this.decisionEngine);
    }

    public void start() throws IOException {
        server = HttpServer.create(new InetSocketAddress(port), 0);
        server.setExecutor(Executors.newVirtualThreadPerTaskExecutor());

        // Context handlers
        server.createContext("/api/health", new HealthHandler());
        server.createContext("/api/evaluate", new EvaluateHandler());
        server.createContext("/api/batch-evaluate", new BatchEvaluateHandler());
        server.createContext("/api/history", new HistoryHandler());
        server.createContext("/api/stress-test", new StressTestHandler());
        server.createContext("/api/rules", new RulesHandler());
        server.createContext("/api/analytics", new AnalyticsHandler());
        server.createContext("/api/reset-samples", new ResetSamplesHandler());
        server.createContext("/api/export", new ExportHandler());

        // Static files handler
        server.createContext("/", new StaticFileHandler());

        server.start();
        System.out.println("==========================================================");
        System.out.println(" CREDIT RISK EVALUATION SYSTEM - SERVER ACTIVE");
        System.out.println(" Web Dashboard: http://localhost:" + port);
        System.out.println(" REST API Base: http://localhost:" + port + "/api");
        System.out.println("==========================================================");
    }

    public void stop() {
        if (server != null) {
            server.stop(1);
        }
    }

    // ==========================================
    // UTILITY METHODS
    // ==========================================

    private static void sendJsonResponse(HttpExchange exchange, int statusCode, Object body) throws IOException {
        String json = JsonUtil.toJson(body);
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);

        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        addCorsHeaders(exchange);
        exchange.sendResponseHeaders(statusCode, bytes.length);

        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private static void addCorsHeaders(HttpExchange exchange) {
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type, Authorization");
    }

    private static String readRequestBody(HttpExchange exchange) throws IOException {
        try (InputStream is = exchange.getRequestBody();
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            byte[] buf = new byte[4096];
            int n;
            while ((n = is.read(buf)) != -1) {
                baos.write(buf, 0, n);
            }
            return baos.toString(StandardCharsets.UTF_8);
        }
    }

    // ==========================================
    // HANDLERS
    // ==========================================

    private class HealthHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                addCorsHeaders(exchange);
                exchange.sendResponseHeaders(204, -1);
                return;
            }
            Map<String, Object> status = new HashMap<>();
            status.put("status", "UP");
            status.put("system", "Enterprise Credit Risk Evaluation System");
            status.put("javaVersion", System.getProperty("java.version"));
            status.put("totalRecords", repository.getAllRecords().size());
            status.put("timestamp", System.currentTimeMillis());
            sendJsonResponse(exchange, 200, status);
        }
    }

    private class EvaluateHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                addCorsHeaders(exchange);
                exchange.sendResponseHeaders(204, -1);
                return;
            }

            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendJsonResponse(exchange, 405, Map.of("error", "Method not allowed. Use POST."));
                return;
            }

            try {
                String body = readRequestBody(exchange);
                Map<String, Object> map = JsonUtil.parseObject(body);
                LoanApplication application = LoanApplication.fromMap(map);

                // Re-sync rules in engine
                decisionEngine.setRules(repository.getRules());
                RiskAssessmentResult result = decisionEngine.evaluate(application);

                // Save to repository
                repository.saveEvaluation(application, result);

                Map<String, Object> response = new HashMap<>();
                response.put("application", application);
                response.put("result", result);
                sendJsonResponse(exchange, 200, response);
            } catch (Exception e) {
                e.printStackTrace();
                sendJsonResponse(exchange, 400, Map.of("error", "Invalid loan application payload: " + e.getMessage()));
            }
        }
    }

    private class BatchEvaluateHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                addCorsHeaders(exchange);
                exchange.sendResponseHeaders(204, -1);
                return;
            }

            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendJsonResponse(exchange, 405, Map.of("error", "Method not allowed. Use POST."));
                return;
            }

            try {
                String body = readRequestBody(exchange);
                List<Object> list = JsonUtil.parseArray(body);
                if (list.isEmpty()) {
                    // Try parsing as object with applications array
                    Map<String, Object> obj = JsonUtil.parseObject(body);
                    list = JsonUtil.getList(obj, "applications");
                }

                decisionEngine.setRules(repository.getRules());
                List<Map<String, Object>> processed = new ArrayList<>();

                for (Object item : list) {
                    if (item instanceof Map<?, ?>) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> m = (Map<String, Object>) item;
                        LoanApplication app = LoanApplication.fromMap(m);
                        RiskAssessmentResult res = decisionEngine.evaluate(app);
                        repository.saveEvaluation(app, res);

                        Map<String, Object> itemMap = new HashMap<>();
                        itemMap.put("application", app);
                        itemMap.put("result", res);
                        processed.add(itemMap);
                    }
                }

                Map<String, Object> resp = new HashMap<>();
                resp.put("count", processed.size());
                resp.put("results", processed);
                resp.put("analytics", repository.getPortfolioAnalytics());

                sendJsonResponse(exchange, 200, resp);
            } catch (Exception e) {
                e.printStackTrace();
                sendJsonResponse(exchange, 400, Map.of("error", "Batch evaluation error: " + e.getMessage()));
            }
        }
    }

    private class HistoryHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                addCorsHeaders(exchange);
                exchange.sendResponseHeaders(204, -1);
                return;
            }

            String path = exchange.getRequestURI().getPath();
            String prefix = "/api/history";

            if (path.length() > prefix.length() && path.charAt(prefix.length()) == '/') {
                String id = path.substring(prefix.length() + 1);
                handleSingleRecord(exchange, id);
                return;
            }

            if ("GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                List<ApplicationRepository.StoredRecord> all = repository.getAllRecords();
                sendJsonResponse(exchange, 200, all);
            } else {
                sendJsonResponse(exchange, 405, Map.of("error", "Method not allowed."));
            }
        }

        private void handleSingleRecord(HttpExchange exchange, String id) throws IOException {
            if ("GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                ApplicationRepository.StoredRecord rec = repository.getRecord(id);
                if (rec == null) {
                    sendJsonResponse(exchange, 404, Map.of("error", "Application record not found: " + id));
                } else {
                    sendJsonResponse(exchange, 200, rec);
                }
            } else if ("DELETE".equalsIgnoreCase(exchange.getRequestMethod())) {
                boolean removed = repository.deleteRecord(id);
                if (removed) {
                    sendJsonResponse(exchange, 200, Map.of("success", true, "deletedId", id));
                } else {
                    sendJsonResponse(exchange, 404, Map.of("error", "Record not found: " + id));
                }
            } else {
                sendJsonResponse(exchange, 405, Map.of("error", "Method not allowed."));
            }
        }
    }

    private class StressTestHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                addCorsHeaders(exchange);
                exchange.sendResponseHeaders(204, -1);
                return;
            }

            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendJsonResponse(exchange, 405, Map.of("error", "Method not allowed. Use POST."));
                return;
            }

            try {
                String body = readRequestBody(exchange);
                Map<String, Object> map = JsonUtil.parseObject(body);

                LoanApplication app;
                String existingId = JsonUtil.getString(map, "applicationId", null);
                if (existingId != null && repository.getRecord(existingId) != null) {
                    app = repository.getRecord(existingId).application;
                } else {
                    app = LoanApplication.fromMap(map);
                }

                decisionEngine.setRules(repository.getRules());
                List<StressTestResult> stressResults = stressTestingEngine.runAllScenarios(app);

                Map<String, Object> res = new HashMap<>();
                res.put("applicationId", app.getId());
                res.put("applicantName", app.getApplicant() != null ? app.getApplicant().getFullName() : "Applicant");
                res.put("scenarios", stressResults);

                sendJsonResponse(exchange, 200, res);
            } catch (Exception e) {
                e.printStackTrace();
                sendJsonResponse(exchange, 400, Map.of("error", "Stress test error: " + e.getMessage()));
            }
        }
    }

    private class RulesHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                addCorsHeaders(exchange);
                exchange.sendResponseHeaders(204, -1);
                return;
            }

            if ("GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendJsonResponse(exchange, 200, repository.getRules());
            } else if ("POST".equalsIgnoreCase(exchange.getRequestMethod()) || "PUT".equalsIgnoreCase(exchange.getRequestMethod())) {
                try {
                    String body = readRequestBody(exchange);
                    Map<String, Object> map = JsonUtil.parseObject(body);
                    UnderwritingRules rules = UnderwritingRules.fromMap(map);
                    repository.saveRules(rules);
                    decisionEngine.setRules(rules);
                    sendJsonResponse(exchange, 200, Map.of("success", true, "rules", rules));
                } catch (Exception e) {
                    sendJsonResponse(exchange, 400, Map.of("error", "Failed to update rules: " + e.getMessage()));
                }
            } else {
                sendJsonResponse(exchange, 405, Map.of("error", "Method not allowed."));
            }
        }
    }

    private class AnalyticsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                addCorsHeaders(exchange);
                exchange.sendResponseHeaders(204, -1);
                return;
            }
            sendJsonResponse(exchange, 200, repository.getPortfolioAnalytics());
        }
    }

    private class ResetSamplesHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                addCorsHeaders(exchange);
                exchange.sendResponseHeaders(204, -1);
                return;
            }
            repository.seedSampleData();
            sendJsonResponse(exchange, 200, Map.of("success", true, "message", "Sample records re-seeded successfully."));
        }
    }

    private class ExportHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                addCorsHeaders(exchange);
                exchange.sendResponseHeaders(204, -1);
                return;
            }

            List<ApplicationRepository.StoredRecord> list = repository.getAllRecords();
            StringBuilder csv = new StringBuilder();
            csv.append("ApplicationID,ApplicantName,AnnualIncome,CreditScore,RequestedAmount,LoanPurpose,TermMonths,MonthlyPayment,DTI,LTV,RiskRating,Decision,PD_Pct,ExpectedLoss\n");

            for (ApplicationRepository.StoredRecord rec : list) {
                Applicant a = rec.application.getApplicant();
                RiskAssessmentResult r = rec.result;
                csv.append(escapeCsv(r.getApplicationId())).append(",");
                csv.append(escapeCsv(r.getApplicantName())).append(",");
                csv.append(a != null ? a.getAnnualIncome() : 0).append(",");
                csv.append(a != null ? a.getCreditScore() : 0).append(",");
                csv.append(rec.application.getLoanAmount()).append(",");
                csv.append(rec.application.getLoanPurpose().name()).append(",");
                csv.append(rec.application.getLoanTermMonths()).append(",");
                csv.append(r.getMonthlyInstallment()).append(",");
                csv.append(r.getDebtToIncomeRatio()).append(",");
                csv.append(r.getLoanToValueRatio()).append(",");
                csv.append(r.getRiskRating().name()).append(",");
                csv.append(r.getDecision().name()).append(",");
                csv.append(Math.round(r.getProbabilityOfDefault() * 10000.0) / 100.0).append(",");
                csv.append(r.getExpectedLoss()).append("\n");
            }

            byte[] bytes = csv.toString().getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "text/csv; charset=UTF-8");
            exchange.getResponseHeaders().set("Content-Disposition", "attachment; filename=\"credit_risk_evaluations.csv\"");
            addCorsHeaders(exchange);
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        }

        private String escapeCsv(String val) {
            if (val == null) return "";
            if (val.contains(",") || val.contains("\"") || val.contains("\n")) {
                return "\"" + val.replace("\"", "\"\"") + "\"";
            }
            return val;
        }
    }

    private static class StaticFileHandler implements HttpHandler {
        private final File baseDir = new File("web");

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();
            if (path == null || path.equals("/") || path.isEmpty()) {
                path = "/index.html";
            }

            // Prevent path traversal
            File target = new File(baseDir, URLDecoder.decode(path.substring(1), StandardCharsets.UTF_8)).getCanonicalFile();
            if (!target.getPath().startsWith(baseDir.getCanonicalPath()) || !target.exists() || target.isDirectory()) {
                // Return index.html fallback for client routing
                target = new File(baseDir, "index.html");
                if (!target.exists()) {
                    String notFound = "404 Not Found";
                    exchange.sendResponseHeaders(404, notFound.length());
                    try (OutputStream os = exchange.getResponseBody()) {
                        os.write(notFound.getBytes(StandardCharsets.UTF_8));
                    }
                    return;
                }
            }

            String mime = getMimeType(target.getName());
            byte[] fileBytes = Files.readAllBytes(target.toPath());

            exchange.getResponseHeaders().set("Content-Type", mime);
            addCorsHeaders(exchange);
            exchange.sendResponseHeaders(200, fileBytes.length);

            try (OutputStream os = exchange.getResponseBody()) {
                os.write(fileBytes);
            }
        }

        private String getMimeType(String filename) {
            String lower = filename.toLowerCase();
            if (lower.endsWith(".html") || lower.endsWith(".htm")) return "text/html; charset=UTF-8";
            if (lower.endsWith(".css")) return "text/css; charset=UTF-8";
            if (lower.endsWith(".js")) return "application/javascript; charset=UTF-8";
            if (lower.endsWith(".json")) return "application/json; charset=UTF-8";
            if (lower.endsWith(".svg")) return "image/svg+xml";
            if (lower.endsWith(".png")) return "image/png";
            if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return "image/jpeg";
            if (lower.endsWith(".ico")) return "image/x-icon";
            return "application/octet-stream";
        }
    }
}
