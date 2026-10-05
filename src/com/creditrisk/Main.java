package com.creditrisk;

import com.creditrisk.cli.CreditRiskCli;
import com.creditrisk.engine.DecisionEngine;
import com.creditrisk.model.LoanApplication;
import com.creditrisk.model.RiskAssessmentResult;
import com.creditrisk.repository.ApplicationRepository;
import com.creditrisk.server.CreditRiskHttpServer;
import com.creditrisk.util.JsonUtil;

import java.io.File;
import java.nio.file.Files;
import java.util.Map;

public class Main {

    public static void main(String[] args) {
        int port = 8080;
        boolean cliMode = false;
        String evalFile = null;

        for (int i = 0; i < args.length; i++) {
            if ("--port".equals(args[i]) && i + 1 < args.length) {
                try {
                    port = Integer.parseInt(args[++i]);
                } catch (NumberFormatException ignored) {}
            } else if ("--cli".equals(args[i])) {
                cliMode = true;
            } else if ("--eval".equals(args[i]) && i + 1 < args.length) {
                evalFile = args[++i];
            } else if ("--help".equals(args[i]) || "-h".equals(args[i])) {
                printHelp();
                return;
            }
        }

        ApplicationRepository repository = new ApplicationRepository();

        if (evalFile != null) {
            runSingleEvaluationFromFile(repository, evalFile);
            return;
        }

        if (cliMode) {
            CreditRiskCli cli = new CreditRiskCli(repository);
            cli.start();
        } else {
            try {
                CreditRiskHttpServer server = new CreditRiskHttpServer(port, repository);
                server.start();

                System.out.println("\n[SYSTEM READY] Open your browser and navigate to:");
                System.out.println(">>> http://localhost:" + port + " <<<\n");
                System.out.println("Press Ctrl+C to stop the server.\n");

                // Keep process alive
                Thread.currentThread().join();
            } catch (Exception e) {
                System.err.println("Fatal error starting Credit Risk Server: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    private static void runSingleEvaluationFromFile(ApplicationRepository repo, String filePath) {
        try {
            File f = new File(filePath);
            if (!f.exists()) {
                System.err.println("File not found: " + filePath);
                return;
            }
            String content = Files.readString(f.toPath());
            Map<String, Object> map = JsonUtil.parseObject(content);
            LoanApplication app = LoanApplication.fromMap(map);
            DecisionEngine engine = new DecisionEngine(repo.getRules());
            RiskAssessmentResult res = engine.evaluate(app);
            System.out.println(JsonUtil.toJson(res));
        } catch (Exception e) {
            System.err.println("Evaluation error: " + e.getMessage());
        }
    }

    private static void printHelp() {
        System.out.println("Credit Risk Evaluation System (Java 25)");
        System.out.println("Usage:");
        System.out.println("  java -cp bin com.creditrisk.Main [options]");
        System.out.println();
        System.out.println("Options:");
        System.out.println("  --port <port>       Set HTTP server port (default: 8080)");
        System.out.println("  --cli               Start interactive command line interface");
        System.out.println("  --eval <file.json>  Evaluate a JSON loan application file");
        System.out.println("  --help, -h          Show this help message");
    }
}
