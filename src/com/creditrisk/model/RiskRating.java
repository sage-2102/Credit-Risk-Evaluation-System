package com.creditrisk.model;

public enum RiskRating {
    AAA("Prime AAA", "Negligible risk of default. Exceptional creditworthiness.", 0.002, "#10b981"),
    AA("Prime AA", "Very low risk of default. High credit quality.", 0.008, "#22c55e"),
    A("Upper Medium A", "Low risk of default. Strong capacity to meet financial commitments.", 0.018, "#84cc16"),
    BBB("Lower Medium BBB", "Moderate risk. Adequate capacity but vulnerable to economic conditions.", 0.045, "#eab308"),
    BB("Speculative BB", "Elevated risk. Speculative elements present, moderate default probability.", 0.095, "#f59e0b"),
    B("Highly Speculative B", "High risk of default. Adverse business conditions likely to impair capacity.", 0.180, "#f97316"),
    CCC("Substantial Risk CCC", "Very high risk of default. Dependent on favorable conditions to survive.", 0.320, "#ef4444"),
    D("Default / Impaired D", "Near certain default or in active distress/bankruptcy.", 0.650, "#991b1b");

    private final String tierName;
    private final String description;
    private final double benchmarkPd;
    private final String badgeColor;

    RiskRating(String tierName, String description, double benchmarkPd, String badgeColor) {
        this.tierName = tierName;
        this.description = description;
        this.benchmarkPd = benchmarkPd;
        this.badgeColor = badgeColor;
    }

    public String getTierName() {
        return tierName;
    }

    public String getDescription() {
        return description;
    }

    public double getBenchmarkPd() {
        return benchmarkPd;
    }

    public String getBadgeColor() {
        return badgeColor;
    }

    public static RiskRating fromScore(double internalScore) {
        if (internalScore >= 850) return AAA;
        if (internalScore >= 780) return AA;
        if (internalScore >= 710) return A;
        if (internalScore >= 640) return BBB;
        if (internalScore >= 560) return BB;
        if (internalScore >= 480) return B;
        if (internalScore >= 380) return CCC;
        return D;
    }
}
