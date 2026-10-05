package com.creditrisk.cli;

import com.creditrisk.engine.DecisionEngine;
import com.creditrisk.engine.StressTestingEngine;
import com.creditrisk.model.*;
import com.creditrisk.repository.ApplicationRepository;

import java.util.List;
import java.util.Map;
import java.util.Scanner;

public class CreditRiskCli {

    private final ApplicationRepository repository;
    private final DecisionEngine engine;
    private final StressTestingEngine stressTestingEngine;
    private final Scanner scanner;

    public CreditRiskCli(ApplicationRepository repository) {
        this.repository = repository;
        this.engine = new DecisionEngine(repository.getRules());
        this.stressTestingEngine = new StressTestingEngine(this.engine);
        this.scanner = new Scanner(System.in);
    }

    public void start() {
        printBanner();
        boolean running = true;

        while (running) {
            System.out.println("\n-----------------------------------------------------------");
            System.out.println(" 1. Evaluate New Loan Application (Interactive)");
            System.out.println(" 2. Run Stress Test (CCAR/Basel Macroeconomic Shock)");
            System.out.println(" 3. View Evaluated Applications & History");
            System.out.println(" 4. View Portfolio Analytics & Value at Risk (VaR)");
            System.out.println(" 5. View Underwriting Rules & Thresholds");
            System.out.println(" 6. Reset & Reload Sample Cases");
            System.out.println(" 0. Exit CLI");
            System.out.println("-----------------------------------------------------------");
            System.out.print(" Select an option [0-6]: ");

            String input = scanner.nextLine().trim();
            switch (input) {
                case "1":
                    handleInteractiveEvaluation();
                    break;
                case "2":
                    handleStressTest();
                    break;
                case "3":
                    handleViewHistory();
                    break;
                case "4":
                    handlePortfolioAnalytics();
                    break;
                case "5":
                    handleViewRules();
                    break;
                case "6":
                    repository.seedSampleData();
                    System.out.println("\n>>> Pre-seeded demo cases successfully reloaded!");
                    break;
                case "0":
                    System.out.println("\nExiting Credit Risk Evaluation System CLI. Goodbye!");
                    running = false;
                    break;
                default:
                    System.out.println("Invalid option. Please enter 0-6.");
            }
        }
    }

    private void printBanner() {
        System.out.println("===============================================================================");
        System.out.println("         CREDIT RISK EVALUATION SYSTEM - ENTERPRISE ENGINE");
        System.out.println("       Multi-Pillar Credit Scoring, PD/LGD Calibration & Basel III");
        System.out.println("===============================================================================");
    }

    private void handleInteractiveEvaluation() {
        System.out.println("\n--- NEW LOAN APPLICATION EVALUATION ---");
        try {
            System.out.print("Applicant Full Name: ");
            String name = scanner.nextLine().trim();
            if (name.isEmpty()) name = "John Doe";

            System.out.print("Applicant Age [21-75]: ");
            int age = Integer.parseInt(promptWithDefault(scanner.nextLine(), "35"));

            System.out.print("Employment Type (SALARIED, BUSINESS_OWNER, SELF_EMPLOYED, FREELANCE, RETIRED): ");
            String empStr = promptWithDefault(scanner.nextLine(), "SALARIED");
            EmploymentType empType = EmploymentType.fromString(empStr);

            System.out.print("Annual Gross Income ($): ");
            double annualIncome = Double.parseDouble(promptWithDefault(scanner.nextLine(), "85000"));

            System.out.print("Existing Monthly Debt Payments ($): ");
            double existingDebt = Double.parseDouble(promptWithDefault(scanner.nextLine(), "450"));

            System.out.print("Monthly Housing Payment / Rent ($): ");
            double housing = Double.parseDouble(promptWithDefault(scanner.nextLine(), "1400"));

            System.out.print("Liquid Assets / Savings ($): ");
            double liquidAssets = Double.parseDouble(promptWithDefault(scanner.nextLine(), "28000"));

            System.out.print("Credit Bureau Score [300-850]: ");
            int creditScore = Integer.parseInt(promptWithDefault(scanner.nextLine(), "720"));

            System.out.print("Delinquencies in Last 2 Years [0-10]: ");
            int delinq = Integer.parseInt(promptWithDefault(scanner.nextLine(), "0"));

            System.out.print("Revolving Credit Utilization % [0-100]: ");
            double util = Double.parseDouble(promptWithDefault(scanner.nextLine(), "25"));

            System.out.print("Credit History Length (years): ");
            double histYears = Double.parseDouble(promptWithDefault(scanner.nextLine(), "7"));

            System.out.print("Requested Loan Amount ($): ");
            double loanAmount = Double.parseDouble(promptWithDefault(scanner.nextLine(), "35000"));

            System.out.print("Loan Purpose (MORTGAGE, AUTO, BUSINESS, PERSONAL, DEBT_CONSOLIDATION, EDUCATION): ");
            String purpStr = promptWithDefault(scanner.nextLine(), "PERSONAL");
            LoanPurpose purpose = LoanPurpose.fromString(purpStr);

            System.out.print("Loan Term in Months (12, 24, 36, 48, 60, 120, 240, 360): ");
            int term = Integer.parseInt(promptWithDefault(scanner.nextLine(), "36"));

            System.out.print("Requested Annual Interest Rate %: ");
            double rate = Double.parseDouble(promptWithDefault(scanner.nextLine(), "8.5"));

            System.out.print("Collateral Type (REAL_ESTATE, VEHICLE, CASH_DEPOSIT, SECURITIES, EQUIPMENT, NONE): ");
            String colStr = promptWithDefault(scanner.nextLine(), "NONE");
            CollateralType colType = CollateralType.fromString(colStr);

            double colVal = 0.0;
            if (colType != CollateralType.NONE) {
                System.out.print("Estimated Collateral Value ($): ");
                colVal = Double.parseDouble(promptWithDefault(scanner.nextLine(), "50000"));
            }

            Applicant applicant = new Applicant();
            applicant.setId("APP-" + (System.currentTimeMillis() % 100000));
            applicant.setFullName(name);
            applicant.setAge(age);
            applicant.setEmploymentType(empType);
            applicant.setAnnualIncome(annualIncome);
            applicant.setExistingMonthlyDebt(existingDebt);
            applicant.setMonthlyHousingPayment(housing);
            applicant.setLiquidAssets(liquidAssets);
            applicant.setCreditScore(creditScore);
            applicant.setDelinquenciesLast2Years(delinq);
            applicant.setRevolvingCreditUtilizationPct(util);
            applicant.setCreditHistoryYears(histYears);

            LoanApplication app = new LoanApplication();
            app.setId("LOAN-" + (System.currentTimeMillis() % 1000000));
            app.setApplicant(applicant);
            app.setLoanAmount(loanAmount);
            app.setLoanPurpose(purpose);
            app.setLoanTermMonths(term);
            app.setRequestedInterestRate(rate);
            app.setCollateralType(colType);
            app.setCollateralValue(colVal);

            RiskAssessmentResult result = engine.evaluate(app);
            repository.saveEvaluation(app, result);

            printEvaluationResult(result, app);

        } catch (Exception e) {
            System.out.println("Error processing evaluation: " + e.getMessage());
        }
    }

    private void printEvaluationResult(RiskAssessmentResult r, LoanApplication app) {
        System.out.println("\n===============================================================================");
        System.out.println("                         EVALUATION AUDIT REPORT");
        System.out.println("===============================================================================");
        System.out.printf(" Application ID : %-20s | Applicant : %s\n", r.getApplicationId(), r.getApplicantName());
        System.out.printf(" Decision       : [%s] | Basel Rating : %s (%s)\n",
            r.getDecision().getLabel().toUpperCase(), r.getRiskRating().name(), r.getRiskRating().getTierName());
        System.out.println("-------------------------------------------------------------------------------");
        System.out.printf(" Composite Risk Score : %6.1f / 1000  | 1-Year Default Prob (PD) : %6.2f%%\n",
            r.getCompositeRiskScore(), r.getProbabilityOfDefault() * 100.0);
        System.out.printf(" Loss Given Default   : %6.1f%%        | Expected Loss (EL)       : $%6.2f\n",
            r.getLossGivenDefault() * 100.0, r.getExpectedLoss());
        System.out.printf(" Exposure at Default  : $%8.2f     | Recommended Interest Rate: %6.2f%%\n",
            r.getExposureAtDefault(), r.getRecommendedInterestRate());
        System.out.println("-------------------------------------------------------------------------------");
        System.out.println(" FINANCIAL CAPACITY & LEVERAGE METRICS:");
        System.out.printf("   * Monthly Payment (EMI)  : $%8.2f\n", r.getMonthlyInstallment());
        System.out.printf("   * Total Debt-to-Income   : %6.1f%%  (Guideline Max: %.1f%%)\n",
            r.getDebtToIncomeRatio(), repository.getRules().getMaxDtiAcceptable());
        System.out.printf("   * Loan-to-Value (LTV)    : %6.1f%%  (Collateral: %s)\n",
            r.getLoanToValueRatio(), app.getCollateralType().getDisplayName());
        System.out.printf("   * Payment-to-Income (PTI): %6.1f%%\n", r.getPaymentToIncomeRatio());
        System.out.printf("   * Liquid Reserves Buffer : %6.1f months\n", r.getLiquidReservesMonths());
        System.out.printf("   * Max Affordable Loan    : $%8.2f\n", r.getMaximumAffordableLoan());
        System.out.println("-------------------------------------------------------------------------------");
        System.out.println(" SCORECARD 5-PILLAR BREAKDOWN (0-100 each):");
        System.out.printf("   1. Credit Bureau & Repayment History : %5.1f / 100 (Weight: 35%%)\n", r.getCreditBureauPillarScore());
        System.out.printf("   2. Capacity & Debt Burdens (DTI/PTI) : %5.1f / 100 (Weight: 25%%)\n", r.getCapacityDebtPillarScore());
        System.out.printf("   3. Capital & Liquid Reserves Buffer  : %5.1f / 100 (Weight: 15%%)\n", r.getCapitalReservesPillarScore());
        System.out.printf("   4. Collateral Coverage & Quality     : %5.1f / 100 (Weight: 15%%)\n", r.getCollateralPillarScore());
        System.out.printf("   5. Employment & Stability Conditions : %5.1f / 100 (Weight: 10%%)\n", r.getStabilityConditionsPillarScore());

        if (!r.getAdverseFactors().isEmpty()) {
            System.out.println("-------------------------------------------------------------------------------");
            System.out.println(" ADVERSE RISK FACTORS / REASON CODES:");
            for (String adv : r.getAdverseFactors()) {
                System.out.println("   [!] " + adv);
            }
        }

        if (!r.getMitigatingFactors().isEmpty()) {
            System.out.println("-------------------------------------------------------------------------------");
            System.out.println(" MITIGATING POSITIVE FACTORS:");
            for (String mit : r.getMitigatingFactors()) {
                System.out.println("   [+] " + mit);
            }
        }

        if (!r.getConditionalRequirements().isEmpty()) {
            System.out.println("-------------------------------------------------------------------------------");
            System.out.println(" UNDERWRITING STIPULATIONS & CLOSING CONDITIONS:");
            for (String cond : r.getConditionalRequirements()) {
                System.out.println("   [*] " + cond);
            }
        }

        System.out.println("-------------------------------------------------------------------------------");
        System.out.println(" MEMORANDUM: " + r.getUnderwriterSummary());
        System.out.println("===============================================================================");
    }

    private void handleStressTest() {
        List<ApplicationRepository.StoredRecord> records = repository.getAllRecords();
        if (records.isEmpty()) {
            System.out.println("No applications available to stress test.");
            return;
        }

        System.out.println("\nSelect an application to Stress-Test:");
        for (int i = 0; i < Math.min(10, records.size()); i++) {
            ApplicationRepository.StoredRecord rec = records.get(i);
            System.out.printf(" [%d] %-12s | %-20s | $%7.0f | Rating: %-3s | Decision: %s\n",
                i + 1, rec.result.getApplicationId(), rec.result.getApplicantName(),
                rec.application.getLoanAmount(), rec.result.getRiskRating().name(), rec.result.getDecision().getLabel());
        }
        System.out.print("Select application [1-" + Math.min(10, records.size()) + "]: ");
        try {
            int sel = Integer.parseInt(scanner.nextLine().trim()) - 1;
            if (sel >= 0 && sel < records.size()) {
                LoanApplication app = records.get(sel).application;
                List<StressTestResult> stressResults = stressTestingEngine.runAllScenarios(app);

                System.out.println("\n===============================================================================");
                System.out.printf(" CCAR/BASEL STRESS TESTING RESULTS FOR: %s ($%.0f)\n",
                    app.getApplicant().getFullName(), app.getLoanAmount());
                System.out.println("===============================================================================");
                System.out.printf("%-24s | %-10s | %-10s | %-12s | %-12s | %s\n",
                    "Scenario", "Stressed EMI", "Stressed DTI", "Stressed PD", "Rating", "Outcome");
                System.out.println("-------------------------------------------------------------------------------");

                for (StressTestResult s : stressResults) {
                    System.out.printf("%-24s | $%9.2f | %9.1f%% | %11.2f%% | %-4s -> %-4s | %s\n",
                        s.getScenarioName(),
                        s.getStressedMonthlyPayment(),
                        s.getStressedDti(),
                        s.getStressedPd() * 100.0,
                        s.getBaseRating().name(),
                        s.getStressedRating().name(),
                        s.isSurvivesStress() ? "PASS" : "FAIL");
                    if (!s.isSurvivesStress()) {
                        System.out.println("   Warning: " + s.getFailureWarning());
                    }
                }
                System.out.println("===============================================================================");
            }
        } catch (Exception e) {
            System.out.println("Invalid selection.");
        }
    }

    private void handleViewHistory() {
        List<ApplicationRepository.StoredRecord> records = repository.getAllRecords();
        System.out.println("\n==========================================================================================");
        System.out.printf(" TOTAL APPLICATIONS ON RECORD: %d\n", records.size());
        System.out.println("==========================================================================================");
        System.out.printf("%-11s | %-20s | %-10s | %-7s | %-7s | %-4s | %-8s | %-16s\n",
            "App ID", "Applicant", "Loan Amt", "Score", "DTI", "Tier", "PD %", "Decision");
        System.out.println("------------------------------------------------------------------------------------------");
        for (ApplicationRepository.StoredRecord rec : records) {
            System.out.printf("%-11s | %-20s | $%9.0f | %-7d | %5.1f%% | %-4s | %6.2f%% | %s\n",
                rec.result.getApplicationId(),
                truncate(rec.result.getApplicantName(), 20),
                rec.application.getLoanAmount(),
                rec.application.getApplicant() != null ? rec.application.getApplicant().getCreditScore() : 0,
                rec.result.getDebtToIncomeRatio(),
                rec.result.getRiskRating().name(),
                rec.result.getProbabilityOfDefault() * 100.0,
                rec.result.getDecision().getLabel());
        }
        System.out.println("==========================================================================================");
    }

    private void handlePortfolioAnalytics() {
        Map<String, Object> a = repository.getPortfolioAnalytics();
        System.out.println("\n===============================================================================");
        System.out.println("                        PORTFOLIO RISK ANALYTICS");
        System.out.println("===============================================================================");
        System.out.printf(" Total Portfolio Applications : %s\n", a.get("totalApplications"));
        System.out.printf(" Total Exposure at Default    : $%s\n", a.get("totalExposure"));
        System.out.printf(" Cumulative Expected Loss     : $%s\n", a.get("totalExpectedLoss"));
        System.out.printf(" Portfolio Weighted Avg PD    : %s%%\n", a.get("averageProbabilityOfDefaultPct"));
        System.out.printf(" Portfolio Avg Credit Score   : %s\n", a.get("averageCreditScore"));
        System.out.printf(" Portfolio Avg Debt-to-Income : %s%%\n", a.get("averageDtiPct"));
        System.out.printf(" Portfolio Value at Risk (95%): $%s\n", a.get("portfolioVaR95"));
        System.out.printf(" Portfolio Value at Risk (99%): $%s\n", a.get("portfolioVaR99"));
        System.out.println("-------------------------------------------------------------------------------");
        System.out.println(" Decision Distribution : " + a.get("decisionDistribution"));
        System.out.println(" Rating Distribution   : " + a.get("ratingDistribution"));
        System.out.println(" Loan Purpose Mix      : " + a.get("purposeDistribution"));
        System.out.println("===============================================================================");
    }

    private void handleViewRules() {
        UnderwritingRules r = repository.getRules();
        System.out.println("\n===============================================================================");
        System.out.println("                     INSTITUTIONAL UNDERWRITING RULES");
        System.out.println("===============================================================================");
        System.out.printf(" Benchmark Risk-Free Rate      : %.2f%%\n", r.getBenchmarkRiskFreeRate());
        System.out.printf(" Min Credit Score Auto-Approve : %d\n", r.getMinCreditScoreAutoApprove());
        System.out.printf(" Min Credit Score Acceptable   : %d\n", r.getMinCreditScoreAcceptable());
        System.out.printf(" Max DTI Auto-Approve          : %.1f%%\n", r.getMaxDtiAutoApprove());
        System.out.printf(" Max DTI Acceptable Ceiling    : %.1f%%\n", r.getMaxDtiAcceptable());
        System.out.printf(" Max LTV Auto-Approve          : %.1f%%\n", r.getMaxLtvAutoApprove());
        System.out.printf(" Max LTV Acceptable Ceiling    : %.1f%%\n", r.getMaxLtvAcceptable());
        System.out.printf(" Min Liquid Reserves Coverage  : %.1f months\n", r.getMinReservesMonths());
        System.out.printf(" Max Revolving Credit Util     : %.1f%%\n", r.getMaxRevolvingUtilAcceptable());
        System.out.printf(" Max Delinquencies (24 mo)     : %d\n", r.getMaxDelinquencies2Y());
        System.out.println("===============================================================================");
    }

    private static String promptWithDefault(String val, String def) {
        return (val == null || val.trim().isEmpty()) ? def : val.trim();
    }

    private static String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, max - 3) + "...";
    }
}
