package com.creditrisk.model;

public enum CollateralType {
    REAL_ESTATE("Real Estate / Residential Property", 0.20, 0.80),
    CASH_DEPOSIT("Cash Deposit / Certificate of Deposit", 0.05, 0.95),
    SECURITIES("Marketable Securities / Stocks / Bonds", 0.25, 0.70),
    VEHICLE("Automobile / Commercial Vehicle", 0.35, 0.65),
    EQUIPMENT("Machinery / Commercial Equipment", 0.45, 0.55),
    NONE("Unsecured / No Collateral", 0.80, 0.00);

    private final String displayName;
    private final double baseLgd;        // Base Loss Given Default
    private final double haircutValue;   // Realizable liquidation haircut multiplier

    CollateralType(String displayName, double baseLgd, double haircutValue) {
        this.displayName = displayName;
        this.baseLgd = baseLgd;
        this.haircutValue = haircutValue;
    }

    public String getDisplayName() {
        return displayName;
    }

    public double getBaseLgd() {
        return baseLgd;
    }

    public double getHaircutValue() {
        return haircutValue;
    }

    public static CollateralType fromString(String text) {
        if (text == null) return NONE;
        for (CollateralType c : values()) {
            if (c.name().equalsIgnoreCase(text) || c.displayName.equalsIgnoreCase(text)) {
                return c;
            }
        }
        return NONE;
    }
}
