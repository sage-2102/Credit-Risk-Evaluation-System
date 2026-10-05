package com.creditrisk.model;

import java.util.Map;
import com.creditrisk.util.JsonUtil;

public class UnderwritingRules {
    private int minCreditScoreAutoApprove = 700;
    private int minCreditScoreAcceptable = 600;
    private double maxDtiAutoApprove = 36.0;         // percent
    private double maxDtiAcceptable = 45.0;           // percent
    private double maxLtvAutoApprove = 80.0;          // percent
    private double maxLtvAcceptable = 95.0;           // percent
    private double minReservesMonths = 2.0;           // months of payment
    private double minEmploymentYears = 1.0;          // years
    private int maxDelinquencies2Y = 1;
    private int maxHardInquiries6M = 3;
    private double maxRevolvingUtilAutoApprove = 35.0; // percent
    private double maxRevolvingUtilAcceptable = 60.0;  // percent
    private double benchmarkRiskFreeRate = 4.5;       // base policy rate %

    public UnderwritingRules() {}

    public static UnderwritingRules fromMap(Map<String, Object> map) {
        UnderwritingRules r = new UnderwritingRules();
        r.setMinCreditScoreAutoApprove(JsonUtil.getInt(map, "minCreditScoreAutoApprove", 700));
        r.setMinCreditScoreAcceptable(JsonUtil.getInt(map, "minCreditScoreAcceptable", 600));
        r.setMaxDtiAutoApprove(JsonUtil.getDouble(map, "maxDtiAutoApprove", 36.0));
        r.setMaxDtiAcceptable(JsonUtil.getDouble(map, "maxDtiAcceptable", 45.0));
        r.setMaxLtvAutoApprove(JsonUtil.getDouble(map, "maxLtvAutoApprove", 80.0));
        r.setMaxLtvAcceptable(JsonUtil.getDouble(map, "maxLtvAcceptable", 95.0));
        r.setMinReservesMonths(JsonUtil.getDouble(map, "minReservesMonths", 2.0));
        r.setMinEmploymentYears(JsonUtil.getDouble(map, "minEmploymentYears", 1.0));
        r.setMaxDelinquencies2Y(JsonUtil.getInt(map, "maxDelinquencies2Y", 1));
        r.setMaxHardInquiries6M(JsonUtil.getInt(map, "maxHardInquiries6M", 3));
        r.setMaxRevolvingUtilAutoApprove(JsonUtil.getDouble(map, "maxRevolvingUtilAutoApprove", 35.0));
        r.setMaxRevolvingUtilAcceptable(JsonUtil.getDouble(map, "maxRevolvingUtilAcceptable", 60.0));
        r.setBenchmarkRiskFreeRate(JsonUtil.getDouble(map, "benchmarkRiskFreeRate", 4.5));
        return r;
    }

    // Getters and Setters
    public int getMinCreditScoreAutoApprove() { return minCreditScoreAutoApprove; }
    public void setMinCreditScoreAutoApprove(int v) { this.minCreditScoreAutoApprove = v; }

    public int getMinCreditScoreAcceptable() { return minCreditScoreAcceptable; }
    public void setMinCreditScoreAcceptable(int v) { this.minCreditScoreAcceptable = v; }

    public double getMaxDtiAutoApprove() { return maxDtiAutoApprove; }
    public void setMaxDtiAutoApprove(double v) { this.maxDtiAutoApprove = v; }

    public double getMaxDtiAcceptable() { return maxDtiAcceptable; }
    public void setMaxDtiAcceptable(double v) { this.maxDtiAcceptable = v; }

    public double getMaxLtvAutoApprove() { return maxLtvAutoApprove; }
    public void setMaxLtvAutoApprove(double v) { this.maxLtvAutoApprove = v; }

    public double getMaxLtvAcceptable() { return maxLtvAcceptable; }
    public void setMaxLtvAcceptable(double v) { this.maxLtvAcceptable = v; }

    public double getMinReservesMonths() { return minReservesMonths; }
    public void setMinReservesMonths(double v) { this.minReservesMonths = v; }

    public double getMinEmploymentYears() { return minEmploymentYears; }
    public void setMinEmploymentYears(double v) { this.minEmploymentYears = v; }

    public int getMaxDelinquencies2Y() { return maxDelinquencies2Y; }
    public void setMaxDelinquencies2Y(int v) { this.maxDelinquencies2Y = v; }

    public int getMaxHardInquiries6M() { return maxHardInquiries6M; }
    public void setMaxHardInquiries6M(int v) { this.maxHardInquiries6M = v; }

    public double getMaxRevolvingUtilAutoApprove() { return maxRevolvingUtilAutoApprove; }
    public void setMaxRevolvingUtilAutoApprove(double v) { this.maxRevolvingUtilAutoApprove = v; }

    public double getMaxRevolvingUtilAcceptable() { return maxRevolvingUtilAcceptable; }
    public void setMaxRevolvingUtilAcceptable(double v) { this.maxRevolvingUtilAcceptable = v; }

    public double getBenchmarkRiskFreeRate() { return benchmarkRiskFreeRate; }
    public void setBenchmarkRiskFreeRate(double v) { this.benchmarkRiskFreeRate = v; }
}
