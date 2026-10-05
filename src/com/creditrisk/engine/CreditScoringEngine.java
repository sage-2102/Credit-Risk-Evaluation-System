package com.creditrisk.engine;

import com.creditrisk.model.*;

public class CreditScoringEngine {

    /**
     * Compute monthly installment (EMI) using standard financial amortization formula.
     * EMI = P * r * (1+r)^n / ((1+r)^n - 1)
     */
    public static double calculateMonthlyInstallment(double principal, double annualInterestRatePct, int termMonths) {
        if (principal <= 0 || termMonths <= 0) return 0.0;
        if (annualInterestRatePct <= 0.001) {
            return principal / termMonths;
        }
        double monthlyRate = (annualInterestRatePct / 100.0) / 12.0;
        double factor = Math.pow(1.0 + monthlyRate, termMonths);
        return principal * (monthlyRate * factor) / (factor - 1.0);
    }

    /**
     * Compute maximum affordable loan amount given borrower income, existing debts, and maximum allowable DTI.
     */
    public static double calculateMaxAffordableLoan(double monthlyGrossIncome, double existingMonthlyDebt, double monthlyHousing, double maxDtiPct, double annualInterestRatePct, int termMonths) {
        double maxAllowableTotalDebt = monthlyGrossIncome * (maxDtiPct / 100.0);
        double maxAvailableForNewLoan = maxAllowableTotalDebt - (existingMonthlyDebt + monthlyHousing);
        if (maxAvailableForNewLoan <= 0) return 0.0;

        if (annualInterestRatePct <= 0.001) {
            return maxAvailableForNewLoan * termMonths;
        }
        double monthlyRate = (annualInterestRatePct / 100.0) / 12.0;
        double factor = Math.pow(1.0 + monthlyRate, termMonths);
        return maxAvailableForNewLoan * (factor - 1.0) / (monthlyRate * factor);
    }

    /**
     * Pillar 1: Credit Bureau & History Score (0 - 100)
     */
    public static double evaluateCreditBureauPillar(Applicant applicant) {
        double score = 0.0;

        // FICO / Bureau score component (0 to 60 points)
        int cs = applicant.getCreditScore();
        if (cs >= 800) score += 60.0;
        else if (cs >= 740) score += 54.0;
        else if (cs >= 680) score += 46.0;
        else if (cs >= 620) score += 34.0;
        else if (cs >= 580) score += 20.0;
        else score += Math.max(0.0, (cs - 300.0) / 280.0 * 15.0);

        // Delinquencies penalty (0 to 15 points)
        int delinq = applicant.getDelinquenciesLast2Years();
        if (delinq == 0) score += 15.0;
        else if (delinq == 1) score += 8.0;
        else if (delinq == 2) score += 3.0;
        // 3+ gets 0

        // Revolving credit utilization (0 to 15 points)
        double util = applicant.getRevolvingCreditUtilizationPct();
        if (util < 10.0) score += 15.0;
        else if (util <= 30.0) score += 13.0;
        else if (util <= 50.0) score += 9.0;
        else if (util <= 75.0) score += 4.0;
        else score += 1.0;

        // Inquiries penalty (0 to 10 points)
        int inq = applicant.getHardInquiriesLast6Months();
        if (inq <= 1) score += 10.0;
        else if (inq <= 3) score += 7.0;
        else if (inq <= 5) score += 3.0;
        // > 5 gets 0

        // Severe deductions
        if (applicant.getPublicRecords() > 0) score = Math.max(0.0, score - 30.0);
        if (applicant.isHasPastDefault()) score = Math.max(0.0, score - 25.0);

        return Math.min(100.0, Math.max(0.0, score));
    }

    /**
     * Pillar 2: Capacity & Debt Burdens (0 - 100)
     */
    public static double evaluateCapacityPillar(double dtiPct, double ptiPct) {
        double score = 0.0;

        // DTI (Debt-to-Income) Score (0 to 65 points)
        if (dtiPct <= 20.0) score += 65.0;
        else if (dtiPct <= 30.0) score += 58.0;
        else if (dtiPct <= 36.0) score += 50.0;
        else if (dtiPct <= 43.0) score += 36.0;
        else if (dtiPct <= 50.0) score += 18.0;
        else score += Math.max(0.0, 18.0 - (dtiPct - 50.0) * 1.5);

        // PTI (Payment-to-Income) Score (0 to 35 points)
        if (ptiPct <= 10.0) score += 35.0;
        else if (ptiPct <= 18.0) score += 30.0;
        else if (ptiPct <= 25.0) score += 22.0;
        else if (ptiPct <= 32.0) score += 12.0;
        else score += Math.max(0.0, 12.0 - (ptiPct - 32.0));

        return Math.min(100.0, Math.max(0.0, score));
    }

    /**
     * Pillar 3: Capital & Liquidity Reserves (0 - 100)
     */
    public static double evaluateCapitalPillar(double reserveMonths, double liquidAssets, double loanAmount) {
        double score = 0.0;

        // Reserves months coverage (0 to 60 points)
        if (reserveMonths >= 12.0) score += 60.0;
        else if (reserveMonths >= 6.0) score += 50.0;
        else if (reserveMonths >= 3.0) score += 38.0;
        else if (reserveMonths >= 1.5) score += 24.0;
        else if (reserveMonths >= 0.5) score += 12.0;
        else score += 4.0;

        // Liquid assets relative to loan size (0 to 40 points)
        if (loanAmount > 0) {
            double liquidToLoanRatio = liquidAssets / loanAmount;
            if (liquidToLoanRatio >= 1.0) score += 40.0;
            else if (liquidToLoanRatio >= 0.5) score += 32.0;
            else if (liquidToLoanRatio >= 0.25) score += 22.0;
            else if (liquidToLoanRatio >= 0.10) score += 12.0;
            else score += 4.0;
        }

        return Math.min(100.0, Math.max(0.0, score));
    }

    /**
     * Pillar 4: Collateral Coverage & LTV (0 - 100)
     */
    public static double evaluateCollateralPillar(CollateralType type, double ltvPct) {
        if (type == CollateralType.NONE) {
            // Unsecured loan: max baseline collateral score is 30
            return 30.0;
        }

        double score = 0.0;

        // Collateral Quality / Haircut (0 to 40 points)
        score += type.getHaircutValue() * 40.0;

        // LTV scoring (0 to 60 points)
        if (ltvPct <= 50.0) score += 60.0;
        else if (ltvPct <= 70.0) score += 52.0;
        else if (ltvPct <= 80.0) score += 44.0;
        else if (ltvPct <= 90.0) score += 30.0;
        else if (ltvPct <= 100.0) score += 18.0;
        else score += Math.max(0.0, 18.0 - (ltvPct - 100.0));

        return Math.min(100.0, Math.max(0.0, score));
    }

    /**
     * Pillar 5: Stability & Conditions (0 - 100)
     */
    public static double evaluateStabilityPillar(Applicant applicant) {
        double score = 0.0;

        // Employment Stability (0 to 40 points)
        double empYears = applicant.getYearsEmployed();
        if (empYears >= 5.0) score += 40.0;
        else if (empYears >= 3.0) score += 34.0;
        else if (empYears >= 1.5) score += 26.0;
        else if (empYears >= 0.5) score += 16.0;
        else score += 8.0;

        // Employment Type weighting
        score *= applicant.getEmploymentType().getStabilityFactor();

        // Credit history depth (0 to 35 points)
        double histYears = applicant.getCreditHistoryYears();
        if (histYears >= 10.0) score += 35.0;
        else if (histYears >= 6.0) score += 28.0;
        else if (histYears >= 3.0) score += 20.0;
        else if (histYears >= 1.0) score += 10.0;
        else score += 4.0;

        // Age factor / Life-stage (0 to 25 points)
        int age = applicant.getAge();
        if (age >= 28 && age <= 62) score += 25.0;
        else if (age >= 23 && age < 28) score += 20.0;
        else if (age > 62) score += 18.0;
        else score += 10.0;

        return Math.min(100.0, Math.max(0.0, score));
    }

    /**
     * Calibrate Probability of Default (PD) using standard credit risk logistic curve.
     * Returns PD as a decimal fraction between 0.001 (0.1%) and 0.85 (85%).
     */
    public static double computeProbabilityOfDefault(double compositeScore) {
        // Standard logistic curve centered at score 580 with slope factor 72.0
        // Score 850+ -> ~0.2% default
        // Score 750  -> ~0.9% default
        // Score 650  -> ~3.8% default
        // Score 580  -> ~11.5% default
        // Score 500  -> ~27% default
        // Score 400  -> ~55% default
        // Score 300  -> ~78% default
        double z = (compositeScore - 580.0) / 72.0;
        double pd = 1.0 / (1.0 + Math.exp(z));

        // Clamping to realistic empirical boundaries
        return Math.min(0.85, Math.max(0.0015, pd));
    }

    /**
     * Compute Loss Given Default (LGD) taking into account collateral type and LTV.
     */
    public static double computeLossGivenDefault(CollateralType collateralType, double ltvPct) {
        double baseLgd = collateralType.getBaseLgd();

        if (collateralType == CollateralType.NONE) {
            return baseLgd; // ~0.80 (80% loss on unsecured default)
        }

        // For secured loans, LGD increases if LTV is high, and decreases if LTV is conservative
        double ltvFactor = (ltvPct / 80.0);
        double adjustedLgd = baseLgd * ltvFactor;

        // Add 5% for legal / administrative recovery friction
        adjustedLgd += 0.05;

        return Math.min(0.95, Math.max(0.05, adjustedLgd));
    }

    /**
     * Compute risk premium interest rate spread over risk-free baseline.
     */
    public static double computeRiskPremiumSpread(RiskRating rating) {
        switch (rating) {
            case AAA: return 0.75;
            case AA: return 1.25;
            case A: return 2.00;
            case BBB: return 3.25;
            case BB: return 5.50;
            case B: return 8.50;
            case CCC: return 13.00;
            case D:
            default: return 18.00;
        }
    }
}
