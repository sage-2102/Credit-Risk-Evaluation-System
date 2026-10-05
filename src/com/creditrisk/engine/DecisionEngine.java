package com.creditrisk.engine;

import com.creditrisk.model.*;
import java.util.ArrayList;
import java.util.List;

public class DecisionEngine {

    private UnderwritingRules rules;

    public DecisionEngine() {
        this.rules = new UnderwritingRules();
    }

    public DecisionEngine(UnderwritingRules rules) {
        this.rules = rules != null ? rules : new UnderwritingRules();
    }

    public UnderwritingRules getRules() {
        return rules;
    }

    public void setRules(UnderwritingRules rules) {
        this.rules = rules;
    }

    public RiskAssessmentResult evaluate(LoanApplication application) {
        Applicant applicant = application.getApplicant();
        if (applicant == null) {
            applicant = new Applicant();
        }

        RiskAssessmentResult result = new RiskAssessmentResult();
        result.setApplicationId(application.getId());
        result.setApplicantName(applicant.getFullName());
        result.setRequestedAmount(application.getLoanAmount());

        // 1. Basic Financial Ratios
        double monthlyIncome = applicant.getMonthlyGrossIncome();
        result.setMonthlyGrossIncome(monthlyIncome);

        double emi = CreditScoringEngine.calculateMonthlyInstallment(
            application.getLoanAmount(),
            application.getRequestedInterestRate(),
            application.getLoanTermMonths()
        );
        result.setMonthlyInstallment(round2(emi));

        double totalRepay = emi * application.getLoanTermMonths();
        result.setTotalRepaymentAmount(round2(totalRepay));
        result.setTotalInterestAmount(round2(Math.max(0, totalRepay - application.getLoanAmount())));

        double totalMonthlyDebt = applicant.getExistingMonthlyDebt() + applicant.getMonthlyHousingPayment() + emi;
        result.setTotalMonthlyDebtObligations(round2(totalMonthlyDebt));

        double dti = monthlyIncome > 0 ? (totalMonthlyDebt / monthlyIncome) * 100.0 : 100.0;
        result.setDebtToIncomeRatio(round2(dti));

        double pti = monthlyIncome > 0 ? (emi / monthlyIncome) * 100.0 : 100.0;
        result.setPaymentToIncomeRatio(round2(pti));

        double ltv = 100.0;
        if (application.getCollateralType() != CollateralType.NONE && application.getCollateralValue() > 0) {
            ltv = (application.getLoanAmount() / application.getCollateralValue()) * 100.0;
        }
        result.setLoanToValueRatio(round2(ltv));

        double reserveMonths = totalMonthlyDebt > 0 ? applicant.getLiquidAssets() / totalMonthlyDebt : 0.0;
        result.setLiquidReservesMonths(round2(reserveMonths));

        // 2. Scorecard Pillar Evaluations (0 - 100 each)
        double creditPillar = CreditScoringEngine.evaluateCreditBureauPillar(applicant);
        double capacityPillar = CreditScoringEngine.evaluateCapacityPillar(dti, pti);
        double capitalPillar = CreditScoringEngine.evaluateCapitalPillar(reserveMonths, applicant.getLiquidAssets(), application.getLoanAmount());
        double collateralPillar = CreditScoringEngine.evaluateCollateralPillar(application.getCollateralType(), ltv);
        double stabilityPillar = CreditScoringEngine.evaluateStabilityPillar(applicant);

        // Co-signer boost
        if (application.isHasCoSigner() && application.getCoSignerCreditScore() >= 700) {
            creditPillar = Math.min(100.0, creditPillar + 10.0);
            capacityPillar = Math.min(100.0, capacityPillar + 5.0);
        }

        result.setCreditBureauPillarScore(round1(creditPillar));
        result.setCapacityDebtPillarScore(round1(capacityPillar));
        result.setCapitalReservesPillarScore(round1(capitalPillar));
        result.setCollateralPillarScore(round1(collateralPillar));
        result.setStabilityConditionsPillarScore(round1(stabilityPillar));

        // 3. Composite Score Calculation (0 - 1000)
        // Weights: Credit 35%, Capacity 25%, Capital 15%, Collateral 15%, Stability 10%
        double compositeScore = (creditPillar * 3.5)
                              + (capacityPillar * 2.5)
                              + (capitalPillar * 1.5)
                              + (collateralPillar * 1.5)
                              + (stabilityPillar * 1.0);
        compositeScore = Math.min(1000.0, Math.max(0.0, compositeScore));
        result.setCompositeRiskScore(round1(compositeScore));

        // 4. Probability of Default & Loss Given Default
        double pd = CreditScoringEngine.computeProbabilityOfDefault(compositeScore);
        double lgd = CreditScoringEngine.computeLossGivenDefault(application.getCollateralType(), ltv);
        double ead = application.getLoanAmount() * 1.02; // accrual margin
        double el = pd * lgd * ead;

        result.setProbabilityOfDefault(round4(pd));
        result.setLossGivenDefault(round4(lgd));
        result.setExposureAtDefault(round2(ead));
        result.setExpectedLoss(round2(el));

        // 5. Basel II/III Risk Rating
        RiskRating rating = RiskRating.fromScore(compositeScore);
        result.setRiskRating(rating);
        result.setRiskRatingTier(rating.getTierName());

        // 6. Pricing Model
        double spread = CreditScoringEngine.computeRiskPremiumSpread(rating);
        result.setRiskPremiumSpread(round2(spread));
        double recommendedRate = rules.getBenchmarkRiskFreeRate() + spread + application.getLoanPurpose().getBaseInterestRate() * 100.0 * 0.2;
        result.setRecommendedInterestRate(round2(recommendedRate));

        // 7. Maximum Affordable Loan Calculation
        double maxAffordable = CreditScoringEngine.calculateMaxAffordableLoan(
            monthlyIncome,
            applicant.getExistingMonthlyDebt(),
            applicant.getMonthlyHousingPayment(),
            rules.getMaxDtiAcceptable(),
            recommendedRate,
            application.getLoanTermMonths()
        );
        result.setMaximumAffordableLoan(round2(maxAffordable));

        // 8. Adverse Factors, Mitigating Factors, and Decision Directives
        List<String> adverse = new ArrayList<>();
        List<String> mitigants = new ArrayList<>();
        List<String> conditions = new ArrayList<>();

        // Check Hard Knockouts
        boolean hardKnockout = false;

        if (applicant.getPublicRecords() > 0) {
            adverse.add("Derogatory public records (bankruptcy, tax lien, judgment) recorded on credit bureau file.");
            hardKnockout = true;
        }

        if (applicant.isHasPastDefault()) {
            adverse.add("Applicant has historic charge-off or debt default record.");
            if (applicant.getCreditScore() < 640) {
                hardKnockout = true;
            }
        }

        if (applicant.getCreditScore() < rules.getMinCreditScoreAcceptable()) {
            adverse.add(String.format("Credit score (%d) is below the minimum institutional underwriting floor (%d).",
                applicant.getCreditScore(), rules.getMinCreditScoreAcceptable()));
            hardKnockout = true;
        }

        if (dti > rules.getMaxDtiAcceptable()) {
            adverse.add(String.format("Debt-to-Income ratio (%.1f%%) exceeds maximum policy threshold (%.1f%%).",
                dti, rules.getMaxDtiAcceptable()));
            if (dti > 52.0 && reserveMonths < 6.0) {
                hardKnockout = true;
            }
        }

        if (application.getCollateralType() != CollateralType.NONE && ltv > rules.getMaxLtvAcceptable()) {
            adverse.add(String.format("Loan-to-Value ratio (%.1f%%) exceeds collateral lending limit (%.1f%%).",
                ltv, rules.getMaxLtvAcceptable()));
        }

        if (applicant.getRevolvingCreditUtilizationPct() > rules.getMaxRevolvingUtilAcceptable()) {
            adverse.add(String.format("High revolving credit line utilization (%.1f%%) signals liquidity strain.",
                applicant.getRevolvingCreditUtilizationPct()));
        }

        if (applicant.getDelinquenciesLast2Years() > rules.getMaxDelinquencies2Y()) {
            adverse.add(String.format("Multiple delinquencies (%d) recorded in the past 24 months.",
                applicant.getDelinquenciesLast2Years()));
        }

        if (reserveMonths < rules.getMinReservesMonths()) {
            adverse.add(String.format("Liquid reserves coverage (%.1f months) is below safety guideline (%.1f months).",
                reserveMonths, rules.getMinReservesMonths()));
        }

        // Mitigating Factors
        if (applicant.getCreditScore() >= 750) {
            mitigants.add(String.format("Tier-1 Credit Score (%d) demonstrating exceptional historical credit discipline.", applicant.getCreditScore()));
        }
        if (dti <= rules.getMaxDtiAutoApprove()) {
            mitigants.add(String.format("Conservative debt profile: DTI of %.1f%% leaves ample disposable income margin.", dti));
        }
        if (reserveMonths >= 6.0) {
            mitigants.add(String.format("Strong liquidity reserve buffer: %.1f months of total debt payments in cash/liquid assets ($%.0f).",
                reserveMonths, applicant.getLiquidAssets()));
        }
        if (applicant.getYearsEmployed() >= 3.0) {
            mitigants.add(String.format("Established employment tenure (%.1f years) with %s stability.",
                applicant.getYearsEmployed(), applicant.getEmploymentType().getDisplayName()));
        }
        if (application.getCollateralType() != CollateralType.NONE && ltv <= 75.0) {
            mitigants.add(String.format("Conservative collateral leverage: Low LTV (%.1f%%) backed by %s.",
                ltv, application.getCollateralType().getDisplayName()));
        }
        if (application.isHasCoSigner() && application.getCoSignerCreditScore() >= 720) {
            mitigants.add(String.format("Credit-worthy co-signer (Score: %d) providing secondary repayment guarantee.", application.getCoSignerCreditScore()));
        }
        if (applicant.getCreditHistoryYears() >= 7.0) {
            mitigants.add(String.format("Deep credit track record (%.1f years) provides high statistical confidence.", applicant.getCreditHistoryYears()));
        }

        // 9. Final Decision Determination
        RiskDecision decision;

        if (hardKnockout) {
            decision = RiskDecision.DECLINED;
        } else if (applicant.getCreditScore() >= rules.getMinCreditScoreAutoApprove()
                && dti <= rules.getMaxDtiAutoApprove()
                && (application.getCollateralType() == CollateralType.NONE || ltv <= rules.getMaxLtvAutoApprove())
                && applicant.getDelinquenciesLast2Years() == 0
                && reserveMonths >= rules.getMinReservesMonths()
                && compositeScore >= 700) {
            decision = RiskDecision.APPROVED;
        } else if (compositeScore >= 550 && dti <= rules.getMaxDtiAcceptable() && applicant.getCreditScore() >= rules.getMinCreditScoreAcceptable()) {
            decision = RiskDecision.CONDITIONAL_APPROVAL;

            // Generate conditional requirements
            if (dti > rules.getMaxDtiAutoApprove()) {
                double targetEmi = (monthlyIncome * (rules.getMaxDtiAutoApprove() / 100.0)) - (applicant.getExistingMonthlyDebt() + applicant.getMonthlyHousingPayment());
                conditions.add(String.format("Condition A: Provide proof of $%.0f in additional liquid reserves, or reduce existing debt to lower DTI below %.0f%%.",
                    applicant.getMonthlyGrossIncome() * 2, rules.getMaxDtiAutoApprove()));
            }
            if (application.getCollateralType() != CollateralType.NONE && ltv > rules.getMaxLtvAutoApprove()) {
                double reqDown = application.getLoanAmount() - (application.getCollateralValue() * (rules.getMaxLtvAutoApprove() / 100.0));
                conditions.add(String.format("Condition B: Increase equity contribution / down payment by $%.0f to bring LTV down to %.0f%%.",
                    Math.max(0, reqDown), rules.getMaxLtvAutoApprove()));
            }
            if (applicant.getRevolvingCreditUtilizationPct() > rules.getMaxRevolvingUtilAutoApprove()) {
                conditions.add("Condition C: Pay down revolving credit cards to bring overall credit utilization below 35% prior to disbursement.");
            }
            if (!application.isHasCoSigner() && compositeScore < 630) {
                conditions.add("Condition D: Furnish a qualifying co-signer with credit score >= 700 to mitigate underwriting risk tier.");
            }
            if (conditions.isEmpty()) {
                conditions.add("Standard closing condition: Verification of employment and latest 2 months of paystubs.");
            }
        } else if (compositeScore >= 450 || (dti <= 50.0 && applicant.getCreditScore() >= 580)) {
            decision = RiskDecision.REFER_MANUAL_REVIEW;
            conditions.add("Referral directive: Escalate to Senior Credit Committee. Manual audit of tax returns (W-2 / 1040), bank statements, and debt explanation letter required.");
        } else {
            decision = RiskDecision.DECLINED;
        }

        result.setDecision(decision);
        result.setAdverseFactors(adverse);
        result.setMitigatingFactors(mitigants);
        result.setConditionalRequirements(conditions);

        // Underwriter Summary Memo
        StringBuilder memo = new StringBuilder();
        memo.append(String.format("Underwriting Assessment: %s with Risk Rating %s (%s). ", decision.getLabel(), rating.name(), rating.getTierName()));
        memo.append(String.format("Composite Score: %.1f/1000, 1-Yr Default Probability: %.2f%%, Expected Loss: $%.2f. ",
            compositeScore, pd * 100.0, el));
        if (decision == RiskDecision.APPROVED) {
            memo.append("Application presents a sound credit risk profile comfortably within risk tolerance appetite.");
        } else if (decision == RiskDecision.CONDITIONAL_APPROVAL) {
            memo.append(String.format("Application meets conditional approval criteria subject to %d stipulation(s).", conditions.size()));
        } else if (decision == RiskDecision.REFER_MANUAL_REVIEW) {
            memo.append("Borderline credit indicators warrant qualitative review by credit committee.");
        } else {
            memo.append("High probability of default and adverse credit factors breach institutional underwriting policy.");
        }
        result.setUnderwriterSummary(memo.toString());

        return result;
    }

    private static double round2(double val) {
        return Math.round(val * 100.0) / 100.0;
    }

    private static double round1(double val) {
        return Math.round(val * 10.0) / 10.0;
    }

    private static double round4(double val) {
        return Math.round(val * 10000.0) / 10000.0;
    }
}
