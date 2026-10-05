package com.creditrisk.model;

import java.util.ArrayList;
import java.util.List;

public class RiskAssessmentResult {
    private String applicationId;
    private String applicantName;
    private String evaluatedAt;

    // Financial Metrics
    private double requestedAmount;
    private double monthlyGrossIncome;
    private double monthlyInstallment;
    private double totalRepaymentAmount;
    private double totalInterestAmount;
    private double totalMonthlyDebtObligations;
    private double debtToIncomeRatio;          // %
    private double paymentToIncomeRatio;       // %
    private double loanToValueRatio;           // %
    private double liquidReservesMonths;

    // Advanced Credit Risk Model Outputs
    private double compositeRiskScore;         // 0 - 1000 internal rating
    private double probabilityOfDefault;       // PD (0.0 to 1.0 or %)
    private double lossGivenDefault;           // LGD (0.0 to 1.0 or %)
    private double exposureAtDefault;          // EAD ($)
    private double expectedLoss;               // EL ($)
    private RiskRating riskRating;             // Basel II/III Rating (AAA -> D)
    private String riskRatingTier;             // Prime, Near Prime, Subprime, etc.
    private RiskDecision decision;             // APPROVED, CONDITIONAL, REFER, DECLINED
    private double recommendedInterestRate;    // %
    private double riskPremiumSpread;          // %
    private double maximumAffordableLoan;      // $

    // Scorecard Pillar Breakdown (0 - 100 each)
    private double creditBureauPillarScore;
    private double capacityDebtPillarScore;
    private double capitalReservesPillarScore;
    private double collateralPillarScore;
    private double stabilityConditionsPillarScore;

    // Explanations & Underwriting Directives
    private List<String> adverseFactors = new ArrayList<>();
    private List<String> mitigatingFactors = new ArrayList<>();
    private List<String> conditionalRequirements = new ArrayList<>();
    private String underwriterSummary;

    public RiskAssessmentResult() {
        this.evaluatedAt = java.time.Instant.now().toString();
    }

    // Getters and Setters
    public String getApplicationId() { return applicationId; }
    public void setApplicationId(String applicationId) { this.applicationId = applicationId; }

    public String getApplicantName() { return applicantName; }
    public void setApplicantName(String applicantName) { this.applicantName = applicantName; }

    public String getEvaluatedAt() { return evaluatedAt; }
    public void setEvaluatedAt(String evaluatedAt) { this.evaluatedAt = evaluatedAt; }

    public double getRequestedAmount() { return requestedAmount; }
    public void setRequestedAmount(double requestedAmount) { this.requestedAmount = requestedAmount; }

    public double getMonthlyGrossIncome() { return monthlyGrossIncome; }
    public void setMonthlyGrossIncome(double monthlyGrossIncome) { this.monthlyGrossIncome = monthlyGrossIncome; }

    public double getMonthlyInstallment() { return monthlyInstallment; }
    public void setMonthlyInstallment(double monthlyInstallment) { this.monthlyInstallment = monthlyInstallment; }

    public double getTotalRepaymentAmount() { return totalRepaymentAmount; }
    public void setTotalRepaymentAmount(double totalRepaymentAmount) { this.totalRepaymentAmount = totalRepaymentAmount; }

    public double getTotalInterestAmount() { return totalInterestAmount; }
    public void setTotalInterestAmount(double totalInterestAmount) { this.totalInterestAmount = totalInterestAmount; }

    public double getTotalMonthlyDebtObligations() { return totalMonthlyDebtObligations; }
    public void setTotalMonthlyDebtObligations(double totalMonthlyDebtObligations) { this.totalMonthlyDebtObligations = totalMonthlyDebtObligations; }

    public double getDebtToIncomeRatio() { return debtToIncomeRatio; }
    public void setDebtToIncomeRatio(double debtToIncomeRatio) { this.debtToIncomeRatio = debtToIncomeRatio; }

    public double getPaymentToIncomeRatio() { return paymentToIncomeRatio; }
    public void setPaymentToIncomeRatio(double paymentToIncomeRatio) { this.paymentToIncomeRatio = paymentToIncomeRatio; }

    public double getLoanToValueRatio() { return loanToValueRatio; }
    public void setLoanToValueRatio(double loanToValueRatio) { this.loanToValueRatio = loanToValueRatio; }

    public double getLiquidReservesMonths() { return liquidReservesMonths; }
    public void setLiquidReservesMonths(double liquidReservesMonths) { this.liquidReservesMonths = liquidReservesMonths; }

    public double getCompositeRiskScore() { return compositeRiskScore; }
    public void setCompositeRiskScore(double compositeRiskScore) { this.compositeRiskScore = compositeRiskScore; }

    public double getProbabilityOfDefault() { return probabilityOfDefault; }
    public void setProbabilityOfDefault(double probabilityOfDefault) { this.probabilityOfDefault = probabilityOfDefault; }

    public double getLossGivenDefault() { return lossGivenDefault; }
    public void setLossGivenDefault(double lossGivenDefault) { this.lossGivenDefault = lossGivenDefault; }

    public double getExposureAtDefault() { return exposureAtDefault; }
    public void setExposureAtDefault(double exposureAtDefault) { this.exposureAtDefault = exposureAtDefault; }

    public double getExpectedLoss() { return expectedLoss; }
    public void setExpectedLoss(double expectedLoss) { this.expectedLoss = expectedLoss; }

    public RiskRating getRiskRating() { return riskRating; }
    public void setRiskRating(RiskRating riskRating) { this.riskRating = riskRating; }

    public String getRiskRatingTier() { return riskRatingTier; }
    public void setRiskRatingTier(String riskRatingTier) { this.riskRatingTier = riskRatingTier; }

    public RiskDecision getDecision() { return decision; }
    public void setDecision(RiskDecision decision) { this.decision = decision; }

    public double getRecommendedInterestRate() { return recommendedInterestRate; }
    public void setRecommendedInterestRate(double recommendedInterestRate) { this.recommendedInterestRate = recommendedInterestRate; }

    public double getRiskPremiumSpread() { return riskPremiumSpread; }
    public void setRiskPremiumSpread(double riskPremiumSpread) { this.riskPremiumSpread = riskPremiumSpread; }

    public double getMaximumAffordableLoan() { return maximumAffordableLoan; }
    public void setMaximumAffordableLoan(double maximumAffordableLoan) { this.maximumAffordableLoan = maximumAffordableLoan; }

    public double getCreditBureauPillarScore() { return creditBureauPillarScore; }
    public void setCreditBureauPillarScore(double score) { this.creditBureauPillarScore = score; }

    public double getCapacityDebtPillarScore() { return capacityDebtPillarScore; }
    public void setCapacityDebtPillarScore(double score) { this.capacityDebtPillarScore = score; }

    public double getCapitalReservesPillarScore() { return capitalReservesPillarScore; }
    public void setCapitalReservesPillarScore(double score) { this.capitalReservesPillarScore = score; }

    public double getCollateralPillarScore() { return collateralPillarScore; }
    public void setCollateralPillarScore(double score) { this.collateralPillarScore = score; }

    public double getStabilityConditionsPillarScore() { return stabilityConditionsPillarScore; }
    public void setStabilityConditionsPillarScore(double score) { this.stabilityConditionsPillarScore = score; }

    public List<String> getAdverseFactors() { return adverseFactors; }
    public void setAdverseFactors(List<String> adverseFactors) { this.adverseFactors = adverseFactors; }

    public List<String> getMitigatingFactors() { return mitigatingFactors; }
    public void setMitigatingFactors(List<String> mitigatingFactors) { this.mitigatingFactors = mitigatingFactors; }

    public List<String> getConditionalRequirements() { return conditionalRequirements; }
    public void setConditionalRequirements(List<String> conditionalRequirements) { this.conditionalRequirements = conditionalRequirements; }

    public String getUnderwriterSummary() { return underwriterSummary; }
    public void setUnderwriterSummary(String underwriterSummary) { this.underwriterSummary = underwriterSummary; }
}
