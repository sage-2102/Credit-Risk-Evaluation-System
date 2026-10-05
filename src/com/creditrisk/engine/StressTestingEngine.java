package com.creditrisk.engine;

import com.creditrisk.model.*;
import java.util.ArrayList;
import java.util.List;

public class StressTestingEngine {

    private final DecisionEngine decisionEngine;

    public StressTestingEngine(DecisionEngine decisionEngine) {
        this.decisionEngine = decisionEngine;
    }

    public List<StressTestResult> runAllScenarios(LoanApplication application) {
        List<StressTestResult> results = new ArrayList<>();
        results.add(evaluateScenario(application, StressTestScenario.getBaseline()));
        results.add(evaluateScenario(application, StressTestScenario.getMildRecession()));
        results.add(evaluateScenario(application, StressTestScenario.getSevereStagflation()));
        results.add(evaluateScenario(application, StressTestScenario.getHousingMarketCrash()));
        return results;
    }

    public StressTestResult evaluateScenario(LoanApplication baseApp, StressTestScenario scenario) {
        // Base evaluation
        RiskAssessmentResult baseRes = decisionEngine.evaluate(baseApp);

        // Create stressed cloned application
        LoanApplication stressedApp = cloneApplication(baseApp);
        Applicant stressedApplicant = stressedApp.getApplicant();

        // 1. Apply interest rate shock
        double shockedRate = stressedApp.getRequestedInterestRate() + (scenario.getInterestRateShockBps() / 100.0);
        stressedApp.setRequestedInterestRate(shockedRate);

        // 2. Apply borrower income shock
        double incomeMultiplier = 1.0 - (scenario.getIncomeDropPct() / 100.0);
        stressedApplicant.setAnnualIncome(stressedApplicant.getAnnualIncome() * incomeMultiplier);

        // 3. Apply collateral valuation shock
        if (stressedApp.getCollateralType() != CollateralType.NONE && stressedApp.getCollateralValue() > 0) {
            double collateralMultiplier = 1.0 - (scenario.getCollateralValueDropPct() / 100.0);
            stressedApp.setCollateralValue(stressedApp.getCollateralValue() * collateralMultiplier);
        }

        // Stressed evaluation
        RiskAssessmentResult stressedRes = decisionEngine.evaluate(stressedApp);

        StressTestResult res = new StressTestResult();
        res.setScenarioName(scenario.getScenarioName());
        res.setBaseMonthlyPayment(baseRes.getMonthlyInstallment());
        res.setStressedMonthlyPayment(stressedRes.getMonthlyInstallment());
        res.setBaseDti(baseRes.getDebtToIncomeRatio());
        res.setStressedDti(stressedRes.getDebtToIncomeRatio());
        res.setBaseLtv(baseRes.getLoanToValueRatio());
        res.setStressedLtv(stressedRes.getLoanToValueRatio());
        res.setBasePd(baseRes.getProbabilityOfDefault());
        res.setStressedPd(stressedRes.getProbabilityOfDefault());
        res.setBaseExpectedLoss(baseRes.getExpectedLoss());
        res.setStressedExpectedLoss(stressedRes.getExpectedLoss());
        res.setBaseRating(baseRes.getRiskRating());
        res.setStressedRating(stressedRes.getRiskRating());

        boolean survives = stressedRes.getDebtToIncomeRatio() <= 50.0
                        && stressedRes.getRiskRating().ordinal() <= RiskRating.B.ordinal()
                        && stressedRes.getDecision() != RiskDecision.DECLINED;
        res.setSurvivesStress(survives);

        if (!survives) {
            StringBuilder warn = new StringBuilder();
            if (stressedRes.getDebtToIncomeRatio() > 50.0) {
                warn.append(String.format("Debt service capacity collapses: Stressed DTI spikes to %.1f%%. ", stressedRes.getDebtToIncomeRatio()));
            }
            if (stressedRes.getLoanToValueRatio() > 100.0 && stressedApp.getCollateralType() != CollateralType.NONE) {
                warn.append(String.format("Collateral underwater: Stressed LTV reaches %.1f%%. ", stressedRes.getLoanToValueRatio()));
            }
            if (stressedRes.getProbabilityOfDefault() > 0.15) {
                warn.append(String.format("Default probability skyrockets to %.2f%%. ", stressedRes.getProbabilityOfDefault() * 100.0));
            }
            res.setFailureWarning(warn.length() > 0 ? warn.toString().trim() : "Severe credit deterioration under macroeconomic stress.");
        } else {
            res.setFailureWarning("Passes stress scenario: Borrower retains debt service headroom.");
        }

        return res;
    }

    private LoanApplication cloneApplication(LoanApplication original) {
        LoanApplication clone = new LoanApplication();
        clone.setId(original.getId());
        clone.setLoanAmount(original.getLoanAmount());
        clone.setLoanPurpose(original.getLoanPurpose());
        clone.setLoanTermMonths(original.getLoanTermMonths());
        clone.setRequestedInterestRate(original.getRequestedInterestRate());
        clone.setCollateralType(original.getCollateralType());
        clone.setCollateralValue(original.getCollateralValue());
        clone.setHasCoSigner(original.isHasCoSigner());
        clone.setCoSignerCreditScore(original.getCoSignerCreditScore());

        Applicant origA = original.getApplicant();
        Applicant cloneA = new Applicant();
        if (origA != null) {
            cloneA.setId(origA.getId());
            cloneA.setFullName(origA.getFullName());
            cloneA.setEmail(origA.getEmail());
            cloneA.setPhone(origA.getPhone());
            cloneA.setAge(origA.getAge());
            cloneA.setEmploymentType(origA.getEmploymentType());
            cloneA.setJobTitle(origA.getJobTitle());
            cloneA.setYearsEmployed(origA.getYearsEmployed());
            cloneA.setAnnualIncome(origA.getAnnualIncome());
            cloneA.setExistingMonthlyDebt(origA.getExistingMonthlyDebt());
            cloneA.setMonthlyHousingPayment(origA.getMonthlyHousingPayment());
            cloneA.setLiquidAssets(origA.getLiquidAssets());
            cloneA.setCreditScore(origA.getCreditScore());
            cloneA.setDelinquenciesLast2Years(origA.getDelinquenciesLast2Years());
            cloneA.setPublicRecords(origA.getPublicRecords());
            cloneA.setHardInquiriesLast6Months(origA.getHardInquiriesLast6Months());
            cloneA.setRevolvingCreditUtilizationPct(origA.getRevolvingCreditUtilizationPct());
            cloneA.setCreditHistoryYears(origA.getCreditHistoryYears());
            cloneA.setHasPastDefault(origA.isHasPastDefault());
        }
        clone.setApplicant(cloneA);
        return clone;
    }
}
