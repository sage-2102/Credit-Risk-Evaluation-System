package com.creditrisk.model;

import java.util.Map;
import com.creditrisk.util.JsonUtil;

public class Applicant {
    private String id;
    private String fullName;
    private String email;
    private String phone;
    private int age;
    private EmploymentType employmentType;
    private String jobTitle;
    private double yearsEmployed;
    private double annualIncome;
    private double existingMonthlyDebt;
    private double monthlyHousingPayment;
    private double liquidAssets;
    private int creditScore;
    private int delinquenciesLast2Years;
    private int publicRecords;
    private int hardInquiriesLast6Months;
    private double revolvingCreditUtilizationPct;
    private double creditHistoryYears;
    private boolean hasPastDefault;

    public Applicant() {
        this.employmentType = EmploymentType.SALARIED;
    }

    public static Applicant fromMap(Map<String, Object> map) {
        Applicant a = new Applicant();
        a.setId(JsonUtil.getString(map, "id", "APP-" + System.currentTimeMillis() % 100000));
        a.setFullName(JsonUtil.getString(map, "fullName", "Anonymous Applicant"));
        a.setEmail(JsonUtil.getString(map, "email", "applicant@example.com"));
        a.setPhone(JsonUtil.getString(map, "phone", "+1 555-0199"));
        a.setAge(JsonUtil.getInt(map, "age", 35));
        a.setEmploymentType(EmploymentType.fromString(JsonUtil.getString(map, "employmentType", "SALARIED")));
        a.setJobTitle(JsonUtil.getString(map, "jobTitle", "Professional"));
        a.setYearsEmployed(JsonUtil.getDouble(map, "yearsEmployed", 4.0));
        a.setAnnualIncome(JsonUtil.getDouble(map, "annualIncome", 75000.0));
        a.setExistingMonthlyDebt(JsonUtil.getDouble(map, "existingMonthlyDebt", 400.0));
        a.setMonthlyHousingPayment(JsonUtil.getDouble(map, "monthlyHousingPayment", 1200.0));
        a.setLiquidAssets(JsonUtil.getDouble(map, "liquidAssets", 25000.0));
        a.setCreditScore(JsonUtil.getInt(map, "creditScore", 710));
        a.setDelinquenciesLast2Years(JsonUtil.getInt(map, "delinquenciesLast2Years", 0));
        a.setPublicRecords(JsonUtil.getInt(map, "publicRecords", 0));
        a.setHardInquiriesLast6Months(JsonUtil.getInt(map, "hardInquiriesLast6Months", 1));
        a.setRevolvingCreditUtilizationPct(JsonUtil.getDouble(map, "revolvingCreditUtilizationPct", 28.0));
        a.setCreditHistoryYears(JsonUtil.getDouble(map, "creditHistoryYears", 8.0));
        a.setHasPastDefault(JsonUtil.getBoolean(map, "hasPastDefault", false));
        return a;
    }

    public double getMonthlyGrossIncome() {
        return annualIncome > 0 ? annualIncome / 12.0 : 0.0;
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public int getAge() { return age; }
    public void setAge(int age) { this.age = age; }

    public EmploymentType getEmploymentType() { return employmentType; }
    public void setEmploymentType(EmploymentType employmentType) { this.employmentType = employmentType; }

    public String getJobTitle() { return jobTitle; }
    public void setJobTitle(String jobTitle) { this.jobTitle = jobTitle; }

    public double getYearsEmployed() { return yearsEmployed; }
    public void setYearsEmployed(double yearsEmployed) { this.yearsEmployed = yearsEmployed; }

    public double getAnnualIncome() { return annualIncome; }
    public void setAnnualIncome(double annualIncome) { this.annualIncome = annualIncome; }

    public double getExistingMonthlyDebt() { return existingMonthlyDebt; }
    public void setExistingMonthlyDebt(double existingMonthlyDebt) { this.existingMonthlyDebt = existingMonthlyDebt; }

    public double getMonthlyHousingPayment() { return monthlyHousingPayment; }
    public void setMonthlyHousingPayment(double monthlyHousingPayment) { this.monthlyHousingPayment = monthlyHousingPayment; }

    public double getLiquidAssets() { return liquidAssets; }
    public void setLiquidAssets(double liquidAssets) { this.liquidAssets = liquidAssets; }

    public int getCreditScore() { return creditScore; }
    public void setCreditScore(int creditScore) { this.creditScore = creditScore; }

    public int getDelinquenciesLast2Years() { return delinquenciesLast2Years; }
    public void setDelinquenciesLast2Years(int delinquenciesLast2Years) { this.delinquenciesLast2Years = delinquenciesLast2Years; }

    public int getPublicRecords() { return publicRecords; }
    public void setPublicRecords(int publicRecords) { this.publicRecords = publicRecords; }

    public int getHardInquiriesLast6Months() { return hardInquiriesLast6Months; }
    public void setHardInquiriesLast6Months(int hardInquiriesLast6Months) { this.hardInquiriesLast6Months = hardInquiriesLast6Months; }

    public double getRevolvingCreditUtilizationPct() { return revolvingCreditUtilizationPct; }
    public void setRevolvingCreditUtilizationPct(double revolvingCreditUtilizationPct) { this.revolvingCreditUtilizationPct = revolvingCreditUtilizationPct; }

    public double getCreditHistoryYears() { return creditHistoryYears; }
    public void setCreditHistoryYears(double creditHistoryYears) { this.creditHistoryYears = creditHistoryYears; }

    public boolean isHasPastDefault() { return hasPastDefault; }
    public void setHasPastDefault(boolean hasPastDefault) { this.hasPastDefault = hasPastDefault; }
}
