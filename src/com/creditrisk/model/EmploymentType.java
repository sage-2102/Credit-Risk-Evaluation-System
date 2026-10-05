package com.creditrisk.model;

public enum EmploymentType {
    SALARIED("Salaried Employee", 1.0),
    BUSINESS_OWNER("Business Owner", 0.90),
    SELF_EMPLOYED("Self Employed / Consultant", 0.85),
    FREELANCE("Freelancer / Gig Economy", 0.75),
    RETIRED("Retired / Pensioner", 0.95),
    UNEMPLOYED("Unemployed", 0.20);

    private final String displayName;
    private final double stabilityFactor;

    EmploymentType(String displayName, double stabilityFactor) {
        this.displayName = displayName;
        this.stabilityFactor = stabilityFactor;
    }

    public String getDisplayName() {
        return displayName;
    }

    public double getStabilityFactor() {
        return stabilityFactor;
    }

    public static EmploymentType fromString(String text) {
        if (text == null) return SALARIED;
        for (EmploymentType e : values()) {
            if (e.name().equalsIgnoreCase(text) || e.displayName.equalsIgnoreCase(text)) {
                return e;
            }
        }
        return SALARIED;
    }
}
