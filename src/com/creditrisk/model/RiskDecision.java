package com.creditrisk.model;

public enum RiskDecision {
    APPROVED("Approved", "Loan application meets all credit and risk underwriting standards.", "#10b981"),
    CONDITIONAL_APPROVAL("Conditional Approval", "Approved subject to specific credit stipulations or mitigations.", "#f59e0b"),
    REFER_MANUAL_REVIEW("Refer for Manual Review", "Borderline application requiring senior underwriter intervention.", "#f97316"),
    DECLINED("Declined", "Application fails credit risk policy thresholds.", "#ef4444");

    private final String label;
    private final String description;
    private final String colorHex;

    RiskDecision(String label, String description, String colorHex) {
        this.label = label;
        this.description = description;
        this.colorHex = colorHex;
    }

    public String getLabel() {
        return label;
    }

    public String getDescription() {
        return description;
    }

    public String getColorHex() {
        return colorHex;
    }
}
