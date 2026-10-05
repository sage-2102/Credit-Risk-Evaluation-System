package com.creditrisk.model;

public enum LoanPurpose {
    MORTGAGE("Home Mortgage / Real Estate", 0.05, 0.85),
    AUTO("Auto Loan / Vehicle Financing", 0.07, 0.90),
    BUSINESS("Small Business / Commercial", 0.085, 0.95),
    DEBT_CONSOLIDATION("Debt Consolidation", 0.09, 1.05),
    PERSONAL("Personal / Unsecured Loan", 0.105, 1.15),
    EDUCATION("Higher Education / Student Loan", 0.065, 0.90),
    HOME_IMPROVEMENT("Home Improvement & Renovation", 0.075, 0.92);

    private final String displayName;
    private final double baseInterestRate;
    private final double riskMultiplier;

    LoanPurpose(String displayName, double baseInterestRate, double riskMultiplier) {
        this.displayName = displayName;
        this.baseInterestRate = baseInterestRate;
        this.riskMultiplier = riskMultiplier;
    }

    public String getDisplayName() {
        return displayName;
    }

    public double getBaseInterestRate() {
        return baseInterestRate;
    }

    public double getRiskMultiplier() {
        return riskMultiplier;
    }

    public static LoanPurpose fromString(String text) {
        if (text == null) return PERSONAL;
        for (LoanPurpose p : values()) {
            if (p.name().equalsIgnoreCase(text) || p.displayName.equalsIgnoreCase(text)) {
                return p;
            }
        }
        return PERSONAL;
    }
}
