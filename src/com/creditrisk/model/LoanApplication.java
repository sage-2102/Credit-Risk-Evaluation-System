package com.creditrisk.model;

import java.time.Instant;
import java.util.Map;
import com.creditrisk.util.JsonUtil;

public class LoanApplication {
    private String id;
    private Applicant applicant;
    private double loanAmount;
    private LoanPurpose loanPurpose;
    private int loanTermMonths;
    private double requestedInterestRate;
    private CollateralType collateralType;
    private double collateralValue;
    private boolean hasCoSigner;
    private int coSignerCreditScore;
    private String createdAt;

    public LoanApplication() {
        this.loanPurpose = LoanPurpose.PERSONAL;
        this.collateralType = CollateralType.NONE;
        this.createdAt = Instant.now().toString();
    }

    public static LoanApplication fromMap(Map<String, Object> map) {
        LoanApplication app = new LoanApplication();
        app.setId(JsonUtil.getString(map, "id", "LOAN-" + System.currentTimeMillis() % 1000000));
        
        Map<String, Object> applicantMap = JsonUtil.getMap(map, "applicant");
        if (!applicantMap.isEmpty()) {
            app.setApplicant(Applicant.fromMap(applicantMap));
        } else {
            // Check if flattened
            app.setApplicant(Applicant.fromMap(map));
        }

        app.setLoanAmount(JsonUtil.getDouble(map, "loanAmount", 25000.0));
        app.setLoanPurpose(LoanPurpose.fromString(JsonUtil.getString(map, "loanPurpose", "PERSONAL")));
        app.setLoanTermMonths(JsonUtil.getInt(map, "loanTermMonths", 36));
        app.setRequestedInterestRate(JsonUtil.getDouble(map, "requestedInterestRate", 8.5));
        app.setCollateralType(CollateralType.fromString(JsonUtil.getString(map, "collateralType", "NONE")));
        app.setCollateralValue(JsonUtil.getDouble(map, "collateralValue", 0.0));
        app.setHasCoSigner(JsonUtil.getBoolean(map, "hasCoSigner", false));
        app.setCoSignerCreditScore(JsonUtil.getInt(map, "coSignerCreditScore", 0));
        app.setCreatedAt(JsonUtil.getString(map, "createdAt", Instant.now().toString()));
        return app;
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public Applicant getApplicant() { return applicant; }
    public void setApplicant(Applicant applicant) { this.applicant = applicant; }

    public double getLoanAmount() { return loanAmount; }
    public void setLoanAmount(double loanAmount) { this.loanAmount = loanAmount; }

    public LoanPurpose getLoanPurpose() { return loanPurpose; }
    public void setLoanPurpose(LoanPurpose loanPurpose) { this.loanPurpose = loanPurpose; }

    public int getLoanTermMonths() { return loanTermMonths; }
    public void setLoanTermMonths(int loanTermMonths) { this.loanTermMonths = loanTermMonths; }

    public double getRequestedInterestRate() { return requestedInterestRate; }
    public void setRequestedInterestRate(double requestedInterestRate) { this.requestedInterestRate = requestedInterestRate; }

    public CollateralType getCollateralType() { return collateralType; }
    public void setCollateralType(CollateralType collateralType) { this.collateralType = collateralType; }

    public double getCollateralValue() { return collateralValue; }
    public void setCollateralValue(double collateralValue) { this.collateralValue = collateralValue; }

    public boolean isHasCoSigner() { return hasCoSigner; }
    public void setHasCoSigner(boolean hasCoSigner) { this.hasCoSigner = hasCoSigner; }

    public int getCoSignerCreditScore() { return coSignerCreditScore; }
    public void setCoSignerCreditScore(int coSignerCreditScore) { this.coSignerCreditScore = coSignerCreditScore; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
}
