package com.creditrisk.model;

import java.util.Map;
import com.creditrisk.util.JsonUtil;

public class StressTestScenario {
    private String scenarioName;
    private String description;
    private double interestRateShockBps;      // e.g. 200 = +2.00%
    private double incomeDropPct;             // e.g. 10.0 = -10%
    private double collateralValueDropPct;    // e.g. 20.0 = -20%
    private double unemploymentRateMultiplier;// e.g. 1.5

    public StressTestScenario(String name, String desc, double rateShockBps, double incomeDrop, double collatDrop) {
        this.scenarioName = name;
        this.description = desc;
        this.interestRateShockBps = rateShockBps;
        this.incomeDropPct = incomeDrop;
        this.collateralValueDropPct = collatDrop;
        this.unemploymentRateMultiplier = 1.0;
    }

    public static StressTestScenario getBaseline() {
        return new StressTestScenario("Baseline Scenario", "Current prevailing market conditions without macroeconomic stress.", 0, 0, 0);
    }

    public static StressTestScenario getMildRecession() {
        return new StressTestScenario("Mild Recession", "GDP contraction with +150 bps interest rate hike, -10% collateral value, -5% income.", 150, 5.0, 10.0);
    }

    public static StressTestScenario getSevereStagflation() {
        return new StressTestScenario("Severe Stagflation", "High inflation with +350 bps rate hike, -25% collateral devaluation, -15% borrower income.", 350, 15.0, 25.0);
    }

    public static StressTestScenario getHousingMarketCrash() {
        return new StressTestScenario("Asset Deflation Shock", "Sharp asset devaluation: -35% collateral property value and +250 bps rate hike.", 250, 10.0, 35.0);
    }

    public static StressTestScenario fromMap(Map<String, Object> map) {
        String name = JsonUtil.getString(map, "scenarioName", "Custom Stress Scenario");
        String desc = JsonUtil.getString(map, "description", "User-defined macroeconomic shock parameters.");
        double rateShock = JsonUtil.getDouble(map, "interestRateShockBps", 200.0);
        double incomeDrop = JsonUtil.getDouble(map, "incomeDropPct", 10.0);
        double collatDrop = JsonUtil.getDouble(map, "collateralValueDropPct", 15.0);
        return new StressTestScenario(name, desc, rateShock, incomeDrop, collatDrop);
    }

    public String getScenarioName() { return scenarioName; }
    public String getDescription() { return description; }
    public double getInterestRateShockBps() { return interestRateShockBps; }
    public double getIncomeDropPct() { return incomeDropPct; }
    public double getCollateralValueDropPct() { return collateralValueDropPct; }
    public double getUnemploymentRateMultiplier() { return unemploymentRateMultiplier; }
}
