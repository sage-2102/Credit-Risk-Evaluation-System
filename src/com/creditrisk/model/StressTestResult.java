package com.creditrisk.model;

public class StressTestResult {
    private String scenarioName;
    private double baseMonthlyPayment;
    private double stressedMonthlyPayment;
    private double baseDti;
    private double stressedDti;
    private double baseLtv;
    private double stressedLtv;
    private double basePd;
    private double stressedPd;
    private double baseExpectedLoss;
    private double stressedExpectedLoss;
    private RiskRating baseRating;
    private RiskRating stressedRating;
    private boolean survivesStress;
    private String failureWarning;

    public StressTestResult() {}

    public String getScenarioName() { return scenarioName; }
    public void setScenarioName(String scenarioName) { this.scenarioName = scenarioName; }

    public double getBaseMonthlyPayment() { return baseMonthlyPayment; }
    public void setBaseMonthlyPayment(double baseMonthlyPayment) { this.baseMonthlyPayment = baseMonthlyPayment; }

    public double getStressedMonthlyPayment() { return stressedMonthlyPayment; }
    public void setStressedMonthlyPayment(double stressedMonthlyPayment) { this.stressedMonthlyPayment = stressedMonthlyPayment; }

    public double getBaseDti() { return baseDti; }
    public void setBaseDti(double baseDti) { this.baseDti = baseDti; }

    public double getStressedDti() { return stressedDti; }
    public void setStressedDti(double stressedDti) { this.stressedDti = stressedDti; }

    public double getBaseLtv() { return baseLtv; }
    public void setBaseLtv(double baseLtv) { this.baseLtv = baseLtv; }

    public double getStressedLtv() { return stressedLtv; }
    public void setStressedLtv(double stressedLtv) { this.stressedLtv = stressedLtv; }

    public double getBasePd() { return basePd; }
    public void setBasePd(double basePd) { this.basePd = basePd; }

    public double getStressedPd() { return stressedPd; }
    public void setStressedPd(double stressedPd) { this.stressedPd = stressedPd; }

    public double getBaseExpectedLoss() { return baseExpectedLoss; }
    public void setBaseExpectedLoss(double baseExpectedLoss) { this.baseExpectedLoss = baseExpectedLoss; }

    public double getStressedExpectedLoss() { return stressedExpectedLoss; }
    public void setStressedExpectedLoss(double stressedExpectedLoss) { this.stressedExpectedLoss = stressedExpectedLoss; }

    public RiskRating getBaseRating() { return baseRating; }
    public void setBaseRating(RiskRating baseRating) { this.baseRating = baseRating; }

    public RiskRating getStressedRating() { return stressedRating; }
    public void setStressedRating(RiskRating stressedRating) { this.stressedRating = stressedRating; }

    public boolean isSurvivesStress() { return survivesStress; }
    public void setSurvivesStress(boolean survivesStress) { this.survivesStress = survivesStress; }

    public String getFailureWarning() { return failureWarning; }
    public void setFailureWarning(String failureWarning) { this.failureWarning = failureWarning; }
}
