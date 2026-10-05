package com.creditrisk.repository;

import com.creditrisk.engine.DecisionEngine;
import com.creditrisk.model.*;
import com.creditrisk.util.JsonUtil;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ApplicationRepository {

    public static class StoredRecord {
        public LoanApplication application;
        public RiskAssessmentResult result;

        public StoredRecord() {}
        public StoredRecord(LoanApplication application, RiskAssessmentResult result) {
            this.application = application;
            this.result = result;
        }
    }

    private final Map<String, StoredRecord> store = new ConcurrentHashMap<>();
    private final File dataFile = new File("data/evaluations.json");
    private final File rulesFile = new File("data/rules.json");
    private UnderwritingRules rules = new UnderwritingRules();

    public ApplicationRepository() {
        loadRules();
        loadFromDisk();
        if (store.isEmpty()) {
            seedSampleData();
        }
    }

    public synchronized void saveEvaluation(LoanApplication application, RiskAssessmentResult result) {
        store.put(result.getApplicationId(), new StoredRecord(application, result));
        saveToDisk();
    }

    public List<StoredRecord> getAllRecords() {
        List<StoredRecord> list = new ArrayList<>(store.values());
        list.sort((a, b) -> {
            String tA = a.result.getEvaluatedAt() != null ? a.result.getEvaluatedAt() : "";
            String tB = b.result.getEvaluatedAt() != null ? b.result.getEvaluatedAt() : "";
            return tB.compareTo(tA);
        });
        return list;
    }

    public StoredRecord getRecord(String id) {
        return store.get(id);
    }

    public synchronized boolean deleteRecord(String id) {
        StoredRecord removed = store.remove(id);
        if (removed != null) {
            saveToDisk();
            return true;
        }
        return false;
    }

    public UnderwritingRules getRules() {
        return rules;
    }

    public synchronized void saveRules(UnderwritingRules newRules) {
        this.rules = newRules;
        try {
            dataFile.getParentFile().mkdirs();
            Files.writeString(rulesFile.toPath(), JsonUtil.toJson(rules), StandardCharsets.UTF_8);
        } catch (Exception e) {
            System.err.println("Could not save rules to disk: " + e.getMessage());
        }
    }

    private void loadRules() {
        try {
            if (rulesFile.exists()) {
                String json = Files.readString(rulesFile.toPath(), StandardCharsets.UTF_8);
                Map<String, Object> map = JsonUtil.parseObject(json);
                this.rules = UnderwritingRules.fromMap(map);
            }
        } catch (Exception e) {
            this.rules = new UnderwritingRules();
        }
    }

    private synchronized void saveToDisk() {
        try {
            dataFile.getParentFile().mkdirs();
            List<Map<String, Object>> list = new ArrayList<>();
            for (StoredRecord rec : store.values()) {
                Map<String, Object> m = new HashMap<>();
                m.put("application", rec.application);
                m.put("result", rec.result);
                list.add(m);
            }
            Files.writeString(dataFile.toPath(), JsonUtil.toJson(list), StandardCharsets.UTF_8);
        } catch (Exception e) {
            System.err.println("Could not persist evaluations to disk: " + e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private void loadFromDisk() {
        try {
            if (!dataFile.exists()) return;
            String json = Files.readString(dataFile.toPath(), StandardCharsets.UTF_8);
            List<Object> rawList = JsonUtil.parseArray(json);
            for (Object obj : rawList) {
                if (obj instanceof Map<?, ?>) {
                    Map<String, Object> map = (Map<String, Object>) obj;
                    Map<String, Object> appMap = JsonUtil.getMap(map, "application");
                    Map<String, Object> resMap = JsonUtil.getMap(map, "result");
                    if (!appMap.isEmpty() && !resMap.isEmpty()) {
                        LoanApplication app = LoanApplication.fromMap(appMap);
                        DecisionEngine engine = new DecisionEngine(this.rules);
                        RiskAssessmentResult res = engine.evaluate(app);
                        res.setApplicationId(JsonUtil.getString(resMap, "applicationId", app.getId()));
                        res.setEvaluatedAt(JsonUtil.getString(resMap, "evaluatedAt", res.getEvaluatedAt()));
                        store.put(res.getApplicationId(), new StoredRecord(app, res));
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error reading evaluations: " + e.getMessage());
        }
    }

    public Map<String, Object> getPortfolioAnalytics() {
        List<StoredRecord> records = getAllRecords();
        int total = records.size();

        double totalExposure = 0.0;
        double totalExpectedLoss = 0.0;
        double sumPd = 0.0;
        double sumCreditScore = 0.0;
        double sumDti = 0.0;
        double sumLtv = 0.0;

        Map<String, Integer> decisionCounts = new LinkedHashMap<>();
        for (RiskDecision d : RiskDecision.values()) {
            decisionCounts.put(d.name(), 0);
        }

        Map<String, Integer> ratingCounts = new LinkedHashMap<>();
        for (RiskRating r : RiskRating.values()) {
            ratingCounts.put(r.name(), 0);
        }

        Map<String, Integer> purposeCounts = new LinkedHashMap<>();

        List<Double> lossValues = new ArrayList<>();

        for (StoredRecord rec : records) {
            RiskAssessmentResult res = rec.result;
            LoanApplication app = rec.application;

            totalExposure += res.getExposureAtDefault();
            totalExpectedLoss += res.getExpectedLoss();
            sumPd += res.getProbabilityOfDefault();
            sumCreditScore += app.getApplicant() != null ? app.getApplicant().getCreditScore() : 650;
            sumDti += res.getDebtToIncomeRatio();
            sumLtv += res.getLoanToValueRatio();

            decisionCounts.put(res.getDecision().name(), decisionCounts.getOrDefault(res.getDecision().name(), 0) + 1);
            ratingCounts.put(res.getRiskRating().name(), ratingCounts.getOrDefault(res.getRiskRating().name(), 0) + 1);

            String purp = app.getLoanPurpose().name();
            purposeCounts.put(purp, purposeCounts.getOrDefault(purp, 0) + 1);

            lossValues.add(res.getExpectedLoss());
        }

        double avgPd = total > 0 ? (sumPd / total) * 100.0 : 0.0;
        double avgCreditScore = total > 0 ? sumCreditScore / total : 0.0;
        double avgDti = total > 0 ? sumDti / total : 0.0;
        double avgLtv = total > 0 ? sumLtv / total : 0.0;

        // Parametric Value at Risk (VaR) estimation
        // Standard normal quantiles: 95% = 1.645, 99% = 2.326
        double stdDev = 0.0;
        if (total > 1) {
            double meanLoss = totalExpectedLoss / total;
            double varianceSum = 0.0;
            for (double l : lossValues) {
                varianceSum += Math.pow(l - meanLoss, 2);
            }
            stdDev = Math.sqrt(varianceSum / (total - 1));
        }
        double var95 = Math.round((totalExpectedLoss + 1.645 * stdDev * Math.sqrt(total)) * 100.0) / 100.0;
        double var99 = Math.round((totalExpectedLoss + 2.326 * stdDev * Math.sqrt(total)) * 100.0) / 100.0;

        Map<String, Object> analytics = new LinkedHashMap<>();
        analytics.put("totalApplications", total);
        analytics.put("totalExposure", Math.round(totalExposure * 100.0) / 100.0);
        analytics.put("totalExpectedLoss", Math.round(totalExpectedLoss * 100.0) / 100.0);
        analytics.put("averageProbabilityOfDefaultPct", Math.round(avgPd * 100.0) / 100.0);
        analytics.put("averageCreditScore", Math.round(avgCreditScore * 10.0) / 10.0);
        analytics.put("averageDtiPct", Math.round(avgDti * 10.0) / 10.0);
        analytics.put("averageLtvPct", Math.round(avgLtv * 10.0) / 10.0);
        analytics.put("portfolioVaR95", var95);
        analytics.put("portfolioVaR99", var99);
        analytics.put("decisionDistribution", decisionCounts);
        analytics.put("ratingDistribution", ratingCounts);
        analytics.put("purposeDistribution", purposeCounts);

        return analytics;
    }

    public synchronized void seedSampleData() {
        DecisionEngine engine = new DecisionEngine(rules);

        // 1. Prime AAA Physician
        createAndSaveSample(engine, "APP-1001", "Dr. Eleanor Vance", "eleanor.vance@metrohealth.org", "+1 555-0101", 42,
            EmploymentType.SALARIED, "Chief of Surgery", 12.0, 290000, 1800, 4200, 180000,
            825, 0, 0, 0, 8.5, 18.0, false,
            "LOAN-9001", 450000, LoanPurpose.MORTGAGE, 360, 5.25, CollateralType.REAL_ESTATE, 850000, false, 0);

        // 2. Strong Business Owner
        createAndSaveSample(engine, "APP-1002", "Marcus Brody", "marcus@brodymanufacturing.com", "+1 555-0102", 48,
            EmploymentType.BUSINESS_OWNER, "Managing Director", 9.0, 165000, 950, 2400, 95000,
            758, 0, 0, 1, 22.0, 14.0, false,
            "LOAN-9002", 90000, LoanPurpose.BUSINESS, 60, 7.50, CollateralType.EQUIPMENT, 160000, false, 0);

        // 3. Tech Engineer - Near Prime (Moderate debt, high income)
        createAndSaveSample(engine, "APP-1003", "Sophia Chen", "sophia.chen@cloudscale.io", "+1 555-0103", 29,
            EmploymentType.SALARIED, "Senior Cloud Architect", 4.5, 135000, 1400, 2800, 32000,
            695, 0, 0, 2, 42.0, 7.0, false,
            "LOAN-9003", 45000, LoanPurpose.AUTO, 48, 6.75, CollateralType.VEHICLE, 58000, false, 0);

        // 4. Young Professional with Co-Signer
        createAndSaveSample(engine, "APP-1004", "Liam O'Connor", "liam.oconnor@creativeagency.net", "+1 555-0104", 25,
            EmploymentType.SALARIED, "Marketing Strategist", 2.0, 62000, 450, 1400, 14000,
            670, 0, 0, 1, 26.0, 4.0, false,
            "LOAN-9004", 28000, LoanPurpose.EDUCATION, 60, 6.25, CollateralType.NONE, 0, true, 770);

        // 5. Freelance Designer - Borderline / Manual Referral
        createAndSaveSample(engine, "APP-1005", "Jackson Reed", "jackson@reedstudios.design", "+1 555-0105", 34,
            EmploymentType.FREELANCE, "Independent UI Specialist", 3.0, 52000, 550, 1250, 9000,
            635, 1, 0, 3, 58.0, 5.0, false,
            "LOAN-9005", 22000, LoanPurpose.PERSONAL, 36, 11.0, CollateralType.NONE, 0, false, 0);

        // 6. Subprime High Risk Borrower - Declined
        createAndSaveSample(engine, "APP-1006", "Tyler Hayes", "tyler.hayes@apexlogistics.com", "+1 555-0106", 31,
            EmploymentType.SALARIED, "Logistics Associate", 1.2, 34000, 850, 1100, 1800,
            535, 3, 0, 6, 88.0, 3.0, true,
            "LOAN-9006", 18000, LoanPurpose.DEBT_CONSOLIDATION, 36, 14.5, CollateralType.NONE, 0, false, 0);

        // 7. Commercial Real Estate Investor
        createAndSaveSample(engine, "APP-1007", "Elena Rostova", "elena@vanguardholdings.com", "+1 555-0107", 52,
            EmploymentType.BUSINESS_OWNER, "Property Developer", 16.0, 220000, 2100, 3500, 210000,
            780, 0, 0, 1, 14.0, 22.0, false,
            "LOAN-9007", 320000, LoanPurpose.MORTGAGE, 240, 5.80, CollateralType.REAL_ESTATE, 650000, false, 0);

        // 8. Public Record Derogatory - Hard Knockout Declined
        createAndSaveSample(engine, "APP-1008", "Chloe Bennett", "chloe.b@fastdeliveries.org", "+1 555-0108", 38,
            EmploymentType.SELF_EMPLOYED, "Independent Contractor", 2.5, 48000, 920, 1300, 2500,
            580, 2, 1, 4, 75.0, 6.0, true,
            "LOAN-9008", 25000, LoanPurpose.PERSONAL, 48, 13.0, CollateralType.NONE, 0, false, 0);

        saveToDisk();
    }

    private void createAndSaveSample(DecisionEngine engine, String appId, String name, String email, String phone, int age,
                                    EmploymentType empType, String title, double yearsEmp, double income,
                                    double debt, double housing, double assets, int creditScore, int delinq,
                                    int pubRec, int inq, double util, double histYears, boolean pastDefault,
                                    String loanId, double loanAmt, LoanPurpose purpose, int term, double rate,
                                    CollateralType colType, double colVal, boolean coSigner, int coScore) {
        Applicant a = new Applicant();
        a.setId(appId);
        a.setFullName(name);
        a.setEmail(email);
        a.setPhone(phone);
        a.setAge(age);
        a.setEmploymentType(empType);
        a.setJobTitle(title);
        a.setYearsEmployed(yearsEmp);
        a.setAnnualIncome(income);
        a.setExistingMonthlyDebt(debt);
        a.setMonthlyHousingPayment(housing);
        a.setLiquidAssets(assets);
        a.setCreditScore(creditScore);
        a.setDelinquenciesLast2Years(delinq);
        a.setPublicRecords(pubRec);
        a.setHardInquiriesLast6Months(inq);
        a.setRevolvingCreditUtilizationPct(util);
        a.setCreditHistoryYears(histYears);
        a.setHasPastDefault(pastDefault);

        LoanApplication app = new LoanApplication();
        app.setId(loanId);
        app.setApplicant(a);
        app.setLoanAmount(loanAmt);
        app.setLoanPurpose(purpose);
        app.setLoanTermMonths(term);
        app.setRequestedInterestRate(rate);
        app.setCollateralType(colType);
        app.setCollateralValue(colVal);
        app.setHasCoSigner(coSigner);
        app.setCoSignerCreditScore(coScore);

        RiskAssessmentResult result = engine.evaluate(app);
        store.put(result.getApplicationId(), new StoredRecord(app, result));
    }
}
