/**
 * Credit Risk Evaluation System - Frontend Controller
 * Supports dual-mode execution:
 * 1. Live Java 25 REST Backend (when running locally at localhost:8080)
 * 2. High-Fidelity Client-Side Engine (when deployed on GitHub Pages static hosting)
 */

const API_BASE = window.location.origin;
let isBackendAvailable = false;

// State management
let portfolioRecords = [];
let portfolioAnalytics = {};
let currentRules = {
  minCreditScoreAutoApprove: 700,
  minCreditScoreAcceptable: 600,
  maxDtiAutoApprove: 36.0,
  maxDtiAcceptable: 45.0,
  maxLtvAutoApprove: 80.0,
  maxLtvAcceptable: 95.0,
  minReservesMonths: 2.0,
  minEmploymentYears: 1.0,
  benchmarkRiskFreeRate: 4.5
};
let currentEvaluationResult = null;
let simulatorDebounceTimer = null;

// Preset sample profiles for instant testing
const PRESET_PROFILES = {
  physician: {
    fullName: "Dr. Eleanor Vance",
    email: "eleanor.vance@metrohealth.org",
    phone: "+1 555-0101",
    age: 42,
    employmentType: "SALARIED",
    jobTitle: "Chief of Surgery",
    yearsEmployed: 12,
    annualIncome: 290000,
    existingMonthlyDebt: 1800,
    monthlyHousingPayment: 4200,
    liquidAssets: 180000,
    creditScore: 825,
    delinquenciesLast2Years: 0,
    publicRecords: 0,
    hardInquiriesLast6Months: 0,
    revolvingCreditUtilizationPct: 8.5,
    creditHistoryYears: 18.0,
    hasPastDefault: false,
    loanAmount: 450000,
    loanPurpose: "MORTGAGE",
    loanTermMonths: 360,
    requestedInterestRate: 5.25,
    collateralType: "REAL_ESTATE",
    collateralValue: 850000,
    hasCoSigner: false,
    coSignerCreditScore: 0
  },
  business_owner: {
    fullName: "Marcus Brody",
    email: "marcus@brodymanufacturing.com",
    phone: "+1 555-0102",
    age: 48,
    employmentType: "BUSINESS_OWNER",
    jobTitle: "Managing Director",
    yearsEmployed: 9,
    annualIncome: 165000,
    existingMonthlyDebt: 950,
    monthlyHousingPayment: 2400,
    liquidAssets: 95000,
    creditScore: 758,
    delinquenciesLast2Years: 0,
    publicRecords: 0,
    hardInquiriesLast6Months: 1,
    revolvingCreditUtilizationPct: 22.0,
    creditHistoryYears: 14.0,
    hasPastDefault: false,
    loanAmount: 90000,
    loanPurpose: "BUSINESS",
    loanTermMonths: 60,
    requestedInterestRate: 7.50,
    collateralType: "EQUIPMENT",
    collateralValue: 160000,
    hasCoSigner: false,
    coSignerCreditScore: 0
  },
  tech_engineer: {
    fullName: "Sophia Chen",
    email: "sophia.chen@cloudscale.io",
    phone: "+1 555-0103",
    age: 29,
    employmentType: "SALARIED",
    jobTitle: "Senior Cloud Architect",
    yearsEmployed: 4.5,
    annualIncome: 135000,
    existingMonthlyDebt: 1400,
    monthlyHousingPayment: 2800,
    liquidAssets: 32000,
    creditScore: 695,
    delinquenciesLast2Years: 0,
    publicRecords: 0,
    hardInquiriesLast6Months: 2,
    revolvingCreditUtilizationPct: 42.0,
    creditHistoryYears: 7.0,
    hasPastDefault: false,
    loanAmount: 45000,
    loanPurpose: "AUTO",
    loanTermMonths: 48,
    requestedInterestRate: 6.75,
    collateralType: "VEHICLE",
    collateralValue: 58000,
    hasCoSigner: false,
    coSignerCreditScore: 0
  },
  freelancer: {
    fullName: "Jackson Reed",
    email: "jackson@reedstudios.design",
    phone: "+1 555-0105",
    age: 34,
    employmentType: "FREELANCE",
    jobTitle: "Independent UI Specialist",
    yearsEmployed: 3.0,
    annualIncome: 52000,
    existingMonthlyDebt: 550,
    monthlyHousingPayment: 1250,
    liquidAssets: 9000,
    creditScore: 635,
    delinquenciesLast2Years: 1,
    publicRecords: 0,
    hardInquiriesLast6Months: 3,
    revolvingCreditUtilizationPct: 58.0,
    creditHistoryYears: 5.0,
    hasPastDefault: false,
    loanAmount: 22000,
    loanPurpose: "PERSONAL",
    loanTermMonths: 36,
    requestedInterestRate: 11.0,
    collateralType: "NONE",
    collateralValue: 0,
    hasCoSigner: false,
    coSignerCreditScore: 0
  },
  subprime: {
    fullName: "Tyler Hayes",
    email: "tyler.hayes@apexlogistics.com",
    phone: "+1 555-0106",
    age: 31,
    employmentType: "SALARIED",
    jobTitle: "Logistics Associate",
    yearsEmployed: 1.2,
    annualIncome: 34000,
    existingMonthlyDebt: 850,
    monthlyHousingPayment: 1100,
    liquidAssets: 1800,
    creditScore: 535,
    delinquenciesLast2Years: 3,
    publicRecords: 0,
    hardInquiriesLast6Months: 6,
    revolvingCreditUtilizationPct: 88.0,
    creditHistoryYears: 3.0,
    hasPastDefault: true,
    loanAmount: 18000,
    loanPurpose: "DEBT_CONSOLIDATION",
    loanTermMonths: 36,
    requestedInterestRate: 14.5,
    collateralType: "NONE",
    collateralValue: 0,
    hasCoSigner: false,
    coSignerCreditScore: 0
  }
};

// Initialize Application
document.addEventListener("DOMContentLoaded", async () => {
  setupNavigation();
  setupEventListeners();
  await checkServerHealth();
  await loadInitialData();
  setupSimulator();
});

// Tab Navigation
function setupNavigation() {
  const tabs = document.querySelectorAll(".nav-tab-btn");
  tabs.forEach(tab => {
    tab.addEventListener("click", () => {
      const targetPaneId = tab.dataset.tab;
      
      tabs.forEach(t => t.classList.remove("active"));
      tab.classList.add("active");

      document.querySelectorAll(".tab-pane").forEach(pane => {
        pane.classList.remove("active");
      });

      const targetPane = document.getElementById(targetPaneId);
      if (targetPane) {
        targetPane.classList.add("active");
      }

      // Refresh specific tab contents
      if (targetPaneId === "pane-dashboard") {
        refreshDashboard();
      } else if (targetPaneId === "pane-history") {
        refreshHistoryTable();
      } else if (targetPaneId === "pane-rules") {
        loadRules();
      } else if (targetPaneId === "pane-stress") {
        populateStressAppDropdown();
      }
    });
  });
}

// Event Listeners
function setupEventListeners() {
  // Preset selector
  const presetSelect = document.getElementById("preset-profile-select");
  if (presetSelect) {
    presetSelect.addEventListener("change", (e) => {
      const key = e.target.value;
      if (key && PRESET_PROFILES[key]) {
        fillEvaluationForm(PRESET_PROFILES[key]);
        showToast("Loaded preset: " + PRESET_PROFILES[key].fullName, "info");
      }
    });
  }

  // Evaluation Form Submit
  const evalForm = document.getElementById("loan-evaluation-form");
  if (evalForm) {
    evalForm.addEventListener("submit", handleEvaluationSubmit);
  }

  // Co-signer toggle
  const cosignerCheck = document.getElementById("input-hasCoSigner");
  if (cosignerCheck) {
    cosignerCheck.addEventListener("change", (e) => {
      const group = document.getElementById("cosigner-score-group");
      if (group) group.style.display = e.target.checked ? "flex" : "none";
    });
  }

  // History search filter
  const historySearch = document.getElementById("history-search-input");
  if (historySearch) {
    historySearch.addEventListener("input", filterHistoryTable);
  }
  const historyDecisionFilter = document.getElementById("history-decision-filter");
  if (historyDecisionFilter) {
    historyDecisionFilter.addEventListener("change", filterHistoryTable);
  }

  // Stress test run button
  const stressRunBtn = document.getElementById("btn-run-stress-test");
  if (stressRunBtn) {
    stressRunBtn.addEventListener("click", handleRunStressTest);
  }

  // Rules form submit
  const rulesForm = document.getElementById("underwriting-rules-form");
  if (rulesForm) {
    rulesForm.addEventListener("submit", handleRulesSave);
  }

  // Batch sample button
  const batchBtn = document.getElementById("btn-run-sample-batch");
  if (batchBtn) {
    batchBtn.addEventListener("click", handleRunSampleBatch);
  }

  // Theme toggle
  const themeToggle = document.getElementById("btn-theme-toggle");
  if (themeToggle) {
    themeToggle.addEventListener("click", toggleTheme);
  }
}

// Check Server Health
async function checkServerHealth() {
  const badge = document.getElementById("server-status-badge");
  try {
    const res = await fetch(`${API_BASE}/api/health`, { signal: AbortSignal.timeout(2000) });
    if (res.ok) {
      isBackendAvailable = true;
      if (badge) {
        badge.innerHTML = `<span class="pulse-dot"></span> Java 25 Engine Online`;
        badge.style.display = "inline-flex";
      }
      return;
    }
  } catch (err) {
    // Backend offline / GitHub Pages static mode
  }

  isBackendAvailable = false;
  if (badge) {
    badge.innerHTML = `<span class="pulse-dot" style="background:#06b6d4;box-shadow:0 0 8px #06b6d4;"></span> Client Engine (GitHub Pages Demo)`;
    badge.style.borderColor = "rgba(6, 182, 212, 0.4)";
    badge.style.color = "#38bdf8";
    badge.style.display = "inline-flex";
  }
}

// Initial Data Load
async function loadInitialData() {
  if (isBackendAvailable) {
    await Promise.all([refreshDashboard(), refreshHistoryTable(), loadRules()]);
  } else {
    // Load or initialize client-side demo records
    loadClientDemoData();
    refreshDashboard();
    refreshHistoryTable();
    loadRules();
  }
  // Populate first preset by default
  fillEvaluationForm(PRESET_PROFILES.physician);
}

// Client demo dataset for GitHub Pages deployment
function loadClientDemoData() {
  const cached = localStorage.getItem("credit_risk_records");
  if (cached) {
    try {
      portfolioRecords = JSON.parse(cached);
      return;
    } catch (e) {}
  }

  // Generate initial records using client engine
  portfolioRecords = [];
  const presets = [
    { p: PRESET_PROFILES.physician, id: "LOAN-9001" },
    { p: PRESET_PROFILES.business_owner, id: "LOAN-9002" },
    { p: PRESET_PROFILES.tech_engineer, id: "LOAN-9003" },
    { p: PRESET_PROFILES.freelancer, id: "LOAN-9005" },
    { p: PRESET_PROFILES.subprime, id: "LOAN-9006" }
  ];

  presets.forEach(item => {
    const res = executeUnderwritingClientEngine(item.p, item.id);
    portfolioRecords.push({ application: { ...item.p, id: item.id, applicant: item.p }, result: res });
  });

  saveClientRecords();
}

function saveClientRecords() {
  try {
    localStorage.setItem("credit_risk_records", JSON.stringify(portfolioRecords));
  } catch (e) {}
}

// Refresh Dashboard Analytics
async function refreshDashboard() {
  if (isBackendAvailable) {
    try {
      const res = await fetch(`${API_BASE}/api/analytics`);
      if (res.ok) {
        portfolioAnalytics = await res.json();
        renderDashboardMetrics(portfolioAnalytics);
        return;
      }
    } catch (err) {}
  }

  // Client-side analytics computation for GitHub Pages
  portfolioAnalytics = computeClientAnalytics(portfolioRecords);
  renderDashboardMetrics(portfolioAnalytics);
}

function computeClientAnalytics(records) {
  const total = records.length;
  let totalExposure = 0;
  let totalExpectedLoss = 0;
  let sumPd = 0;
  let sumCreditScore = 0;

  const decisionCounts = { APPROVED: 0, CONDITIONAL_APPROVAL: 0, REFER_MANUAL_REVIEW: 0, DECLINED: 0 };
  const ratingCounts = { AAA: 0, AA: 0, A: 0, BBB: 0, BB: 0, B: 0, CCC: 0, D: 0 };
  const losses = [];

  records.forEach(rec => {
    const r = rec.result;
    const a = rec.application.applicant || rec.application;
    totalExposure += (r.exposureAtDefault || 0);
    totalExpectedLoss += (r.expectedLoss || 0);
    sumPd += (r.probabilityOfDefault || 0);
    sumCreditScore += (a.creditScore || 650);

    if (decisionCounts[r.decision] !== undefined) decisionCounts[r.decision]++;
    if (ratingCounts[r.riskRating] !== undefined) ratingCounts[r.riskRating]++;
    losses.push(r.expectedLoss || 0);
  });

  const avgPd = total > 0 ? (sumPd / total) * 100 : 0;
  const avgCreditScore = total > 0 ? (sumCreditScore / total) : 0;

  let stdDev = 0;
  if (total > 1) {
    const mean = totalExpectedLoss / total;
    const varSum = losses.reduce((acc, v) => acc + Math.pow(v - mean, 2), 0);
    stdDev = Math.sqrt(varSum / (total - 1));
  }
  const var95 = totalExpectedLoss + 1.645 * stdDev * Math.sqrt(total);
  const var99 = totalExpectedLoss + 2.326 * stdDev * Math.sqrt(total);

  return {
    totalApplications: total,
    totalExposure,
    totalExpectedLoss,
    averageProbabilityOfDefaultPct: avgPd,
    averageCreditScore: avgCreditScore,
    portfolioVaR95: var95,
    portfolioVaR99: var99,
    decisionDistribution: decisionCounts,
    ratingDistribution: ratingCounts
  };
}

function renderDashboardMetrics(data) {
  document.getElementById("metric-total-apps").textContent = data.totalApplications || 0;
  document.getElementById("metric-total-exposure").textContent = "$" + formatCurrency(data.totalExposure || 0);
  document.getElementById("metric-expected-loss").textContent = "$" + formatCurrency(data.totalExpectedLoss || 0);
  document.getElementById("metric-avg-pd").textContent = (data.averageProbabilityOfDefaultPct || 0).toFixed(2) + "%";
  document.getElementById("metric-var-95").textContent = "$" + formatCurrency(data.portfolioVaR95 || 0);
  document.getElementById("metric-var-99").textContent = "$" + formatCurrency(data.portfolioVaR99 || 0);

  // Ratings breakdown bar chart
  const ratingDist = data.ratingDistribution || {};
  const ratingContainer = document.getElementById("rating-distribution-bars");
  if (ratingContainer) {
    ratingContainer.innerHTML = "";
    const ratings = ["AAA", "AA", "A", "BBB", "BB", "B", "CCC", "D"];
    const colors = {
      AAA: "#10b981", AA: "#22c55e", A: "#84cc16",
      BBB: "#eab308", BB: "#f59e0b", B: "#f97316",
      CCC: "#ef4444", D: "#991b1b"
    };
    const maxCount = Math.max(...Object.values(ratingDist), 1);

    ratings.forEach(r => {
      const count = ratingDist[r] || 0;
      const pct = (count / maxCount) * 100;
      const div = document.createElement("div");
      div.className = "pillar-row";
      div.innerHTML = `
        <div class="pillar-meta">
          <span class="pillar-name" style="font-weight:700;">Tier ${r}</span>
          <span class="pillar-score-text">${count} loan(s)</span>
        </div>
        <div class="pillar-bar-track">
          <div class="pillar-bar-fill" style="width: ${pct}%; background: ${colors[r] || '#3b82f6'};"></div>
        </div>
      `;
      ratingContainer.appendChild(div);
    });
  }

  // Decision breakdown
  const decDist = data.decisionDistribution || {};
  document.getElementById("stat-approved-count").textContent = decDist.APPROVED || 0;
  document.getElementById("stat-conditional-count").textContent = decDist.CONDITIONAL_APPROVAL || 0;
  document.getElementById("stat-refer-count").textContent = decDist.REFER_MANUAL_REVIEW || 0;
  document.getElementById("stat-declined-count").textContent = decDist.DECLINED || 0;
}

// Fill Form Helper
function fillEvaluationForm(profile) {
  for (const [key, value] of Object.entries(profile)) {
    const el = document.getElementById(`input-${key}`);
    if (el) {
      if (el.type === "checkbox") {
        el.checked = Boolean(value);
      } else {
        el.value = value;
      }
    }
  }
  const cosignerGroup = document.getElementById("cosigner-score-group");
  if (cosignerGroup) {
    cosignerGroup.style.display = profile.hasCoSigner ? "flex" : "none";
  }
}

// Extract Application from Form
function extractFormApplication() {
  const getVal = (id, def = "") => {
    const el = document.getElementById(id);
    return el ? el.value : def;
  };
  const getNum = (id, def = 0) => {
    const val = parseFloat(getVal(id, def));
    return isNaN(val) ? def : val;
  };
  const getInt = (id, def = 0) => {
    const val = parseInt(getVal(id, def), 10);
    return isNaN(val) ? def : val;
  };

  return {
    fullName: getVal("input-fullName", "Applicant"),
    email: getVal("input-email", "test@example.com"),
    phone: getVal("input-phone", "+1 555-0199"),
    age: getInt("input-age", 35),
    employmentType: getVal("input-employmentType", "SALARIED"),
    jobTitle: getVal("input-jobTitle", "Professional"),
    yearsEmployed: getNum("input-yearsEmployed", 4.0),
    annualIncome: getNum("input-annualIncome", 75000),
    existingMonthlyDebt: getNum("input-existingMonthlyDebt", 400),
    monthlyHousingPayment: getNum("input-monthlyHousingPayment", 1200),
    liquidAssets: getNum("input-liquidAssets", 25000),
    creditScore: getInt("input-creditScore", 710),
    delinquenciesLast2Years: getInt("input-delinquenciesLast2Years", 0),
    publicRecords: getInt("input-publicRecords", 0),
    hardInquiriesLast6Months: getInt("input-hardInquiriesLast6Months", 1),
    revolvingCreditUtilizationPct: getNum("input-revolvingCreditUtilizationPct", 28.0),
    creditHistoryYears: getNum("input-creditHistoryYears", 8.0),
    hasPastDefault: document.getElementById("input-hasPastDefault")?.checked || false,

    loanAmount: getNum("input-loanAmount", 25000),
    loanPurpose: getVal("input-loanPurpose", "PERSONAL"),
    loanTermMonths: getInt("input-loanTermMonths", 36),
    requestedInterestRate: getNum("input-requestedInterestRate", 8.5),
    collateralType: getVal("input-collateralType", "NONE"),
    collateralValue: getNum("input-collateralValue", 0),
    hasCoSigner: document.getElementById("input-hasCoSigner")?.checked || false,
    coSignerCreditScore: getInt("input-coSignerCreditScore", 0)
  };
}

// Handle Evaluation Submit
async function handleEvaluationSubmit(e) {
  e.preventDefault();
  const btn = document.getElementById("btn-submit-evaluation");
  btn.disabled = true;
  btn.innerHTML = `<span class="pulse-dot"></span> Underwriting in progress...`;

  try {
    const payload = extractFormApplication();
    let resultData = null;

    if (isBackendAvailable) {
      try {
        const res = await fetch(`${API_BASE}/api/evaluate`, {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify(payload)
        });
        if (res.ok) {
          resultData = await res.json();
        }
      } catch (err) {
        // Fall back to client engine if backend fails
      }
    }

    if (!resultData) {
      // Execute client-side engine (GitHub Pages)
      const res = executeUnderwritingClientEngine(payload, "LOAN-" + Math.floor(100000 + Math.random() * 900000));
      resultData = { application: { ...payload, id: res.applicationId, applicant: payload }, result: res };
      portfolioRecords.unshift(resultData);
      saveClientRecords();
    }

    currentEvaluationResult = resultData;
    renderEvaluationResults(resultData.result, resultData.application);
    showToast(`Loan ${resultData.result.decision} - Score: ${resultData.result.compositeRiskScore}`, "success");
    
    refreshDashboard();
    refreshHistoryTable();
    document.getElementById("evaluation-result-section").scrollIntoView({ behavior: "smooth" });
  } catch (err) {
    showToast(err.message, "error");
  } finally {
    btn.disabled = false;
    btn.innerHTML = `<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z"/></svg> Execute Underwriting Assessment`;
  }
}

// Client-Side Underwriting Engine (Identical to Java 25 CreditScoringEngine)
function executeUnderwritingClientEngine(app, loanId) {
  const annualIncome = app.annualIncome || 0;
  const monthlyIncome = annualIncome > 0 ? annualIncome / 12 : 0;
  const loanAmt = app.loanAmount || 0;
  const ratePct = app.requestedInterestRate || 8.0;
  const termMonths = app.loanTermMonths || 36;
  const monthlyRate = (ratePct / 100) / 12;
  const factor = Math.pow(1 + monthlyRate, termMonths);
  const emi = (monthlyRate > 0 && factor > 1) ? loanAmt * (monthlyRate * factor) / (factor - 1) : loanAmt / termMonths;
  const totalRepay = emi * termMonths;
  const totalMonthlyDebt = (app.existingMonthlyDebt || 0) + (app.monthlyHousingPayment || 0) + emi;
  const dti = monthlyIncome > 0 ? (totalMonthlyDebt / monthlyIncome) * 100 : 100;
  const pti = monthlyIncome > 0 ? (emi / monthlyIncome) * 100 : 100;

  let ltv = 100;
  if (app.collateralType && app.collateralType !== "NONE" && app.collateralValue > 0) {
    ltv = (loanAmt / app.collateralValue) * 100;
  }
  const reserveMonths = totalMonthlyDebt > 0 ? (app.liquidAssets || 0) / totalMonthlyDebt : 0;

  // 1. Credit Bureau Pillar (0-100)
  let creditScore = 0;
  const cs = app.creditScore || 650;
  if (cs >= 800) creditScore += 60;
  else if (cs >= 740) creditScore += 54;
  else if (cs >= 680) creditScore += 46;
  else if (cs >= 620) creditScore += 34;
  else if (cs >= 580) creditScore += 20;
  else creditScore += Math.max(0, (cs - 300) / 280 * 15);

  const delinq = app.delinquenciesLast2Years || 0;
  if (delinq === 0) creditScore += 15;
  else if (delinq === 1) creditScore += 8;
  else if (delinq === 2) creditScore += 3;

  const util = app.revolvingCreditUtilizationPct || 30;
  if (util < 10) creditScore += 15;
  else if (util <= 30) creditScore += 13;
  else if (util <= 50) creditScore += 9;
  else if (util <= 75) creditScore += 4;
  else creditScore += 1;

  const inq = app.hardInquiriesLast6Months || 0;
  if (inq <= 1) creditScore += 10;
  else if (inq <= 3) creditScore += 7;
  else if (inq <= 5) creditScore += 3;

  if (app.publicRecords > 0) creditScore = Math.max(0, creditScore - 30);
  if (app.hasPastDefault) creditScore = Math.max(0, creditScore - 25);
  creditScore = Math.min(100, Math.max(0, creditScore));

  // 2. Capacity Pillar (0-100)
  let capacityScore = 0;
  if (dti <= 20) capacityScore += 65;
  else if (dti <= 30) capacityScore += 58;
  else if (dti <= 36) capacityScore += 50;
  else if (dti <= 43) capacityScore += 36;
  else if (dti <= 50) capacityScore += 18;
  else capacityScore += Math.max(0, 18 - (dti - 50) * 1.5);

  if (pti <= 10) capacityScore += 35;
  else if (pti <= 18) capacityScore += 30;
  else if (pti <= 25) capacityScore += 22;
  else if (pti <= 32) capacityScore += 12;
  else capacityScore += Math.max(0, 12 - (pti - 32));
  capacityScore = Math.min(100, Math.max(0, capacityScore));

  // 3. Capital Reserves Pillar (0-100)
  let capitalScore = 0;
  if (reserveMonths >= 12) capitalScore += 60;
  else if (reserveMonths >= 6) capitalScore += 50;
  else if (reserveMonths >= 3) capitalScore += 38;
  else if (reserveMonths >= 1.5) capitalScore += 24;
  else if (reserveMonths >= 0.5) capitalScore += 12;
  else capitalScore += 4;

  const liquidRatio = loanAmt > 0 ? (app.liquidAssets || 0) / loanAmt : 0;
  if (liquidRatio >= 1.0) capitalScore += 40;
  else if (liquidRatio >= 0.5) capitalScore += 32;
  else if (liquidRatio >= 0.25) capitalScore += 22;
  else if (liquidRatio >= 0.10) capitalScore += 12;
  else capitalScore += 4;
  capitalScore = Math.min(100, Math.max(0, capitalScore));

  // 4. Collateral Pillar (0-100)
  let colScore = 30;
  if (app.collateralType && app.collateralType !== "NONE") {
    const haircuts = { REAL_ESTATE: 0.80, CASH_DEPOSIT: 0.95, SECURITIES: 0.70, VEHICLE: 0.65, EQUIPMENT: 0.55 };
    colScore = (haircuts[app.collateralType] || 0.5) * 40;
    if (ltv <= 50) colScore += 60;
    else if (ltv <= 70) colScore += 52;
    else if (ltv <= 80) colScore += 44;
    else if (ltv <= 90) colScore += 30;
    else if (ltv <= 100) colScore += 18;
    else colScore += Math.max(0, 18 - (ltv - 100));
  }
  colScore = Math.min(100, Math.max(0, colScore));

  // 5. Stability Pillar (0-100)
  let stabilityScore = 0;
  const empY = app.yearsEmployed || 2;
  if (empY >= 5) stabilityScore += 40;
  else if (empY >= 3) stabilityScore += 34;
  else if (empY >= 1.5) stabilityScore += 26;
  else if (empY >= 0.5) stabilityScore += 16;
  else stabilityScore += 8;

  const empTypeWeights = { SALARIED: 1.0, BUSINESS_OWNER: 0.9, SELF_EMPLOYED: 0.85, FREELANCE: 0.75, RETIRED: 0.95, UNEMPLOYED: 0.2 };
  stabilityScore *= (empTypeWeights[app.employmentType] || 0.85);

  const histY = app.creditHistoryYears || 5;
  if (histY >= 10) stabilityScore += 35;
  else if (histY >= 6) stabilityScore += 28;
  else if (histY >= 3) stabilityScore += 20;
  else if (histY >= 1) stabilityScore += 10;
  else stabilityScore += 4;
  stabilityScore = Math.min(100, Math.max(0, stabilityScore + 20));

  if (app.hasCoSigner && app.coSignerCreditScore >= 700) {
    creditScore = Math.min(100, creditScore + 10);
    capacityScore = Math.min(100, capacityScore + 5);
  }

  // Composite Score (0 - 1000)
  const compositeScore = Math.round(((creditScore * 3.5) + (capacityScore * 2.5) + (capitalScore * 1.5) + (colScore * 1.5) + (stabilityScore * 1.0)) * 10) / 10;

  // Calibrated Probability of Default (PD)
  const z = (compositeScore - 580) / 72.0;
  const pd = Math.min(0.85, Math.max(0.0015, 1.0 / (1.0 + Math.exp(z))));

  // LGD
  const baseLgdMap = { REAL_ESTATE: 0.20, CASH_DEPOSIT: 0.05, SECURITIES: 0.25, VEHICLE: 0.35, EQUIPMENT: 0.45, NONE: 0.80 };
  let lgd = baseLgdMap[app.collateralType] || 0.80;
  if (app.collateralType && app.collateralType !== "NONE") {
    lgd = Math.min(0.95, Math.max(0.05, lgd * (ltv / 80.0) + 0.05));
  }
  const ead = loanAmt * 1.02;
  const el = pd * lgd * ead;

  // Basel Rating
  let rating = "D";
  let tierName = "Default / Impaired D";
  if (compositeScore >= 850) { rating = "AAA"; tierName = "Prime AAA"; }
  else if (compositeScore >= 780) { rating = "AA"; tierName = "Prime AA"; }
  else if (compositeScore >= 710) { rating = "A"; tierName = "Upper Medium A"; }
  else if (compositeScore >= 640) { rating = "BBB"; tierName = "Lower Medium BBB"; }
  else if (compositeScore >= 560) { rating = "BB"; tierName = "Speculative BB"; }
  else if (compositeScore >= 480) { rating = "B"; tierName = "Highly Speculative B"; }
  else if (compositeScore >= 380) { rating = "CCC"; tierName = "Substantial Risk CCC"; }

  // Decision & Factors
  const adverse = [];
  const mitigants = [];
  const conditions = [];
  let hardKnockout = false;

  if (app.publicRecords > 0) { adverse.push("Derogatory public records (bankruptcy, tax lien) recorded on credit file."); hardKnockout = true; }
  if (app.hasPastDefault && cs < 640) { adverse.push("Applicant has historic debt default record with sub-640 credit score."); hardKnockout = true; }
  if (cs < currentRules.minCreditScoreAcceptable) { adverse.push(`Credit score (${cs}) is below institutional floor (${currentRules.minCreditScoreAcceptable}).`); hardKnockout = true; }
  if (dti > currentRules.maxDtiAcceptable) { adverse.push(`Debt-to-Income ratio (${dti.toFixed(1)}%) exceeds policy ceiling (${currentRules.maxDtiAcceptable}%).`); if (dti > 52) hardKnockout = true; }
  if (util > 60) adverse.push(`High revolving credit line utilization (${util.toFixed(1)}%) signals liquidity strain.`);
  if (delinq > 1) adverse.push(`Multiple delinquencies (${delinq}) recorded in past 24 months.`);

  if (cs >= 750) mitigants.push(`Tier-1 Credit Score (${cs}) demonstrating exceptional repayment discipline.`);
  if (dti <= 36) mitigants.push(`Conservative debt profile: DTI of ${dti.toFixed(1)}% leaves ample cash-flow margin.`);
  if (reserveMonths >= 6) mitigants.push(`Strong liquidity reserve buffer: ${reserveMonths.toFixed(1)} months of debt payments in liquid assets.`);
  if (empY >= 3) mitigants.push(`Established employment tenure (${empY.toFixed(1)} years).`);

  let decision = "DECLINED";
  if (hardKnockout) {
    decision = "DECLINED";
  } else if (cs >= currentRules.minCreditScoreAutoApprove && dti <= currentRules.maxDtiAutoApprove && delinq === 0 && compositeScore >= 700) {
    decision = "APPROVED";
  } else if (compositeScore >= 550 && dti <= currentRules.maxDtiAcceptable && cs >= currentRules.minCreditScoreAcceptable) {
    decision = "CONDITIONAL_APPROVAL";
    if (dti > currentRules.maxDtiAutoApprove) conditions.push(`Condition A: Provide proof of additional reserves to lower DTI below ${currentRules.maxDtiAutoApprove}%.`);
    if (ltv > currentRules.maxLtvAutoApprove) conditions.push(`Condition B: Increase down payment to bring LTV below ${currentRules.maxLtvAutoApprove}%.`);
    if (conditions.length === 0) conditions.push("Standard closing condition: Verification of employment and latest paystubs.");
  } else if (compositeScore >= 450 || (dti <= 50 && cs >= 580)) {
    decision = "REFER_MANUAL_REVIEW";
    conditions.push("Referral directive: Escalate to Senior Credit Committee for manual tax returns and debt audit.");
  }

  const spreads = { AAA: 0.75, AA: 1.25, A: 2.0, BBB: 3.25, BB: 5.5, B: 8.5, CCC: 13.0, D: 18.0 };
  const recommendedRate = currentRules.benchmarkRiskFreeRate + (spreads[rating] || 3.0);
  const maxAffordable = monthlyIncome > 0 ? (monthlyIncome * 0.40 - ((app.existingMonthlyDebt || 0) + (app.monthlyHousingPayment || 0))) * 30 : 0;

  return {
    applicationId: loanId || ("LOAN-" + Math.floor(100000 + Math.random() * 900000)),
    applicantName: app.fullName || "Applicant",
    evaluatedAt: new Date().toISOString(),
    requestedAmount: loanAmt,
    monthlyGrossIncome: monthlyIncome,
    monthlyInstallment: Math.round(emi * 100) / 100,
    totalRepaymentAmount: Math.round(totalRepay * 100) / 100,
    debtToIncomeRatio: Math.round(dti * 10) / 10,
    loanToValueRatio: Math.round(ltv * 10) / 10,
    liquidReservesMonths: Math.round(reserveMonths * 10) / 10,
    compositeRiskScore: compositeScore,
    probabilityOfDefault: Math.round(pd * 10000) / 10000,
    lossGivenDefault: Math.round(lgd * 1000) / 1000,
    exposureAtDefault: Math.round(ead * 100) / 100,
    expectedLoss: Math.round(el * 100) / 100,
    riskRating: rating,
    riskRatingTier: tierName,
    decision: decision,
    recommendedInterestRate: Math.round(recommendedRate * 100) / 100,
    maximumAffordableLoan: Math.max(0, Math.round(maxAffordable)),
    creditBureauPillarScore: Math.round(creditScore * 10) / 10,
    capacityDebtPillarScore: Math.round(capacityScore * 10) / 10,
    capitalReservesPillarScore: Math.round(capitalScore * 10) / 10,
    collateralPillarScore: Math.round(colScore * 10) / 10,
    stabilityConditionsPillarScore: Math.round(stabilityScore * 10) / 10,
    adverseFactors: adverse,
    mitigatingFactors: mitigants,
    conditionalRequirements: conditions,
    underwriterSummary: `Underwriting Assessment: ${formatDecisionLabel(decision)} with Risk Rating ${rating} (${tierName}). Composite Score: ${compositeScore}/1000, 1-Yr Default Probability: ${(pd * 100).toFixed(2)}%, Expected Loss: $${formatCurrency(el)}.`
  };
}

// Render Evaluation Results
function renderEvaluationResults(r, app) {
  const section = document.getElementById("evaluation-result-section");
  section.style.display = "block";

  // Decision Banner
  const banner = document.getElementById("res-decision-banner");
  banner.className = `decision-banner ${r.decision.toLowerCase()}`;
  
  const iconMap = {
    APPROVED: "✓",
    CONDITIONAL_APPROVAL: "⚠",
    REFER_MANUAL_REVIEW: "⧖",
    DECLINED: "✕"
  };
  document.getElementById("res-decision-icon").textContent = iconMap[r.decision] || "•";
  document.getElementById("res-decision-title").textContent = formatDecisionLabel(r.decision);
  document.getElementById("res-decision-desc").textContent = r.underwriterSummary;

  const ratingBadge = document.getElementById("res-rating-badge");
  ratingBadge.textContent = `${r.riskRating} (${r.riskRatingTier})`;
  ratingBadge.style.background = getRatingColor(r.riskRating);
  ratingBadge.style.color = "#fff";

  // Financial Metrics
  document.getElementById("res-emi").textContent = "$" + formatCurrency(r.monthlyInstallment);
  document.getElementById("res-total-repay").textContent = "$" + formatCurrency(r.totalRepaymentAmount);
  document.getElementById("res-dti").textContent = r.debtToIncomeRatio.toFixed(1) + "%";
  document.getElementById("res-ltv").textContent = r.loanToValueRatio.toFixed(1) + "%";
  document.getElementById("res-reserves").textContent = r.liquidReservesMonths.toFixed(1) + " mos";
  document.getElementById("res-max-affordable").textContent = "$" + formatCurrency(r.maximumAffordableLoan);
  document.getElementById("res-rec-rate").textContent = r.recommendedInterestRate.toFixed(2) + "%";
  document.getElementById("res-expected-loss").textContent = "$" + formatCurrency(r.expectedLoss);
  document.getElementById("res-pd").textContent = (r.probabilityOfDefault * 100).toFixed(2) + "%";
  document.getElementById("res-lgd").textContent = (r.lossGivenDefault * 100).toFixed(1) + "%";

  // Composite Score Gauge
  renderScoreGauge(r.compositeRiskScore);

  // 5 Pillars
  renderPillarBar("pillar-credit", r.creditBureauPillarScore);
  renderPillarBar("pillar-capacity", r.capacityDebtPillarScore);
  renderPillarBar("pillar-capital", r.capitalReservesPillarScore);
  renderPillarBar("pillar-collateral", r.collateralPillarScore);
  renderPillarBar("pillar-stability", r.stabilityConditionsPillarScore);

  // Adverse Factors
  const advList = document.getElementById("res-adverse-list");
  advList.innerHTML = "";
  if (r.adverseFactors && r.adverseFactors.length > 0) {
    r.adverseFactors.forEach(f => {
      const li = document.createElement("li");
      li.className = "factor-item adverse";
      li.innerHTML = `<strong>[!]</strong> <span>${f}</span>`;
      advList.appendChild(li);
    });
  } else {
    advList.innerHTML = `<li class="factor-item mitigant">✓ No negative adverse action factors detected.</li>`;
  }

  // Mitigating Factors
  const mitList = document.getElementById("res-mitigating-list");
  mitList.innerHTML = "";
  if (r.mitigatingFactors && r.mitigatingFactors.length > 0) {
    r.mitigatingFactors.forEach(f => {
      const li = document.createElement("li");
      li.className = "factor-item mitigant";
      li.innerHTML = `<strong>[+]</strong> <span>${f}</span>`;
      mitList.appendChild(li);
    });
  } else {
    mitList.innerHTML = `<li class="factor-item" style="color:var(--text-muted);">No significant positive mitigants noted.</li>`;
  }

  // Conditions
  const condList = document.getElementById("res-conditions-list");
  condList.innerHTML = "";
  if (r.conditionalRequirements && r.conditionalRequirements.length > 0) {
    r.conditionalRequirements.forEach(f => {
      const li = document.createElement("li");
      li.className = "factor-item condition";
      li.innerHTML = `<strong>[*]</strong> <span>${f}</span>`;
      condList.appendChild(li);
    });
  } else {
    condList.innerHTML = `<li class="factor-item" style="color:var(--text-muted);">No additional underwriting closing conditions stipulated.</li>`;
  }
}

// Render SVG Score Gauge (0 - 1000)
function renderScoreGauge(score) {
  const scoreNum = document.getElementById("gauge-score-value");
  if (scoreNum) scoreNum.textContent = Math.round(score);

  const needle = document.getElementById("gauge-needle");
  if (needle) {
    const deg = -90 + (score / 1000) * 180;
    needle.setAttribute("transform", `rotate(${deg} 70 70)`);
  }
}

function renderPillarBar(id, score) {
  const el = document.getElementById(id);
  const text = document.getElementById(`${id}-val`);
  if (el) el.style.width = Math.min(100, Math.max(0, score)) + "%";
  if (text) text.textContent = score.toFixed(1) + " / 100";
}

// History Table
async function refreshHistoryTable() {
  if (isBackendAvailable) {
    try {
      const res = await fetch(`${API_BASE}/api/history`);
      if (res.ok) {
        portfolioRecords = await res.json();
      }
    } catch (err) {}
  }
  renderHistoryRows(portfolioRecords);
  populateStressAppDropdown();
}

function renderHistoryRows(records) {
  const tbody = document.getElementById("history-table-body");
  if (!tbody) return;
  tbody.innerHTML = "";

  if (records.length === 0) {
    tbody.innerHTML = `<tr><td colspan="8" style="text-align:center;padding:2rem;color:var(--text-muted);">No applications recorded yet.</td></tr>`;
    return;
  }

  records.forEach(rec => {
    const r = rec.result;
    const a = rec.application.applicant || rec.application;
    const tr = document.createElement("tr");
    tr.innerHTML = `
      <td><strong style="color:var(--accent-cyan);">${r.applicationId}</strong></td>
      <td>
        <div><strong>${r.applicantName}</strong></div>
        <div style="font-size:0.75rem;color:var(--text-muted);">${a ? (a.jobTitle || a.employmentType) : ''}</div>
      </td>
      <td><strong>$${formatCurrency(rec.application.loanAmount)}</strong></td>
      <td><span style="font-family:'JetBrains Mono';">${a ? a.creditScore : '-'}</span></td>
      <td><span style="font-family:'JetBrains Mono';">${r.debtToIncomeRatio.toFixed(1)}%</span></td>
      <td><span class="badge-tag" style="background:${getRatingColor(r.riskRating)};color:#fff;">${r.riskRating}</span></td>
      <td><span class="status-chip ${r.decision.toLowerCase()}">${formatDecisionLabel(r.decision)}</span></td>
      <td>
        <button class="btn btn-secondary btn-sm" onclick="viewAuditMemo('${r.applicationId}')">Memo</button>
        <button class="btn btn-danger btn-sm" onclick="deleteApplication('${r.applicationId}')">✕</button>
      </td>
    `;
    tbody.appendChild(tr);
  });
}

function filterHistoryTable() {
  const query = (document.getElementById("history-search-input")?.value || "").toLowerCase();
  const decision = document.getElementById("history-decision-filter")?.value || "ALL";

  const filtered = portfolioRecords.filter(rec => {
    const nameMatch = rec.result.applicantName.toLowerCase().includes(query) ||
                      rec.result.applicationId.toLowerCase().includes(query);
    const decisionMatch = (decision === "ALL" || rec.result.decision === decision);
    return nameMatch && decisionMatch;
  });

  renderHistoryRows(filtered);
}

// Modal View Audit Memo
window.viewAuditMemo = function(appId) {
  const rec = portfolioRecords.find(x => x.result.applicationId === appId);
  if (!rec) return;

  const r = rec.result;
  const app = rec.application;
  const a = app.applicant || app;

  const content = document.getElementById("modal-memo-content");
  content.innerHTML = `
    <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:1rem;border-bottom:1px solid var(--border-subtle);padding-bottom:0.75rem;">
      <div>
        <h2 style="font-size:1.4rem;">Credit Risk Underwriting Memorandum</h2>
        <div style="color:var(--text-muted);font-size:0.8rem;">Application ID: ${r.applicationId} | Evaluated: ${new Date(r.evaluatedAt).toLocaleString()}</div>
      </div>
      <div>
        <span class="status-chip ${r.decision.toLowerCase()}" style="font-size:0.95rem;padding:0.4rem 1rem;">${formatDecisionLabel(r.decision)}</span>
      </div>
    </div>

    <div class="form-grid-3" style="margin-bottom:1.5rem;">
      <div class="metric-card" style="padding:0.85rem;">
        <div class="metric-label">Applicant</div>
        <div style="font-size:1.1rem;font-weight:700;">${r.applicantName}</div>
        <div style="font-size:0.75rem;color:var(--text-muted);">${a ? (a.jobTitle || a.employmentType) : ''}</div>
      </div>
      <div class="metric-card" style="padding:0.85rem;">
        <div class="metric-label">Requested Loan</div>
        <div style="font-size:1.1rem;font-weight:700;color:var(--accent-cyan);">$${formatCurrency(app.loanAmount)}</div>
        <div style="font-size:0.75rem;color:var(--text-muted);">${app.loanPurpose || 'Personal'} • ${app.loanTermMonths} Mos @ ${app.requestedInterestRate}%</div>
      </div>
      <div class="metric-card" style="padding:0.85rem;">
        <div class="metric-label">Basel Rating</div>
        <div style="font-size:1.1rem;font-weight:700;color:${getRatingColor(r.riskRating)};">${r.riskRating} (${r.riskRatingTier})</div>
        <div style="font-size:0.75rem;color:var(--text-muted);">PD: ${(r.probabilityOfDefault * 100).toFixed(2)}% | EL: $${formatCurrency(r.expectedLoss)}</div>
      </div>
    </div>

    <div style="background:rgba(0,0,0,0.25);border-radius:var(--radius-md);padding:1rem;margin-bottom:1.25rem;">
      <h4 style="font-size:0.85rem;text-transform:uppercase;color:var(--accent-blue);margin-bottom:0.5rem;">Underwriter Executive Summary</h4>
      <p style="font-size:0.9rem;line-height:1.6;">${r.underwriterSummary}</p>
    </div>

    <div class="content-grid-2">
      <div>
        <h4 style="font-size:0.85rem;text-transform:uppercase;color:#ef4444;margin-bottom:0.5rem;">Adverse Risk Factors</h4>
        <ul class="factor-list">
          ${(r.adverseFactors && r.adverseFactors.length > 0) ? r.adverseFactors.map(x => `<li class="factor-item adverse">${x}</li>`).join('') : '<li class="factor-item mitigant">None</li>'}
        </ul>
      </div>
      <div>
        <h4 style="font-size:0.85rem;text-transform:uppercase;color:#10b981;margin-bottom:0.5rem;">Mitigating Strengths</h4>
        <ul class="factor-list">
          ${(r.mitigatingFactors && r.mitigatingFactors.length > 0) ? r.mitigatingFactors.map(x => `<li class="factor-item mitigant">${x}</li>`).join('') : '<li class="factor-item" style="color:var(--text-muted);">None noted</li>'}
        </ul>
      </div>
    </div>

    <div style="margin-top:1.25rem;">
      <h4 style="font-size:0.85rem;text-transform:uppercase;color:#f59e0b;margin-bottom:0.5rem;">Stipulations & Conditions</h4>
      <ul class="factor-list">
        ${(r.conditionalRequirements && r.conditionalRequirements.length > 0) ? r.conditionalRequirements.map(x => `<li class="factor-item condition">${x}</li>`).join('') : '<li class="factor-item" style="color:var(--text-muted);">Standard closing verification only.</li>'}
      </ul>
    </div>

    <div style="margin-top:1.5rem;display:flex;justify-content:flex-end;gap:0.75rem;">
      <button class="btn btn-secondary" onclick="window.print()">Print / Export Memo</button>
      <button class="btn btn-primary" onclick="closeModal()">Close</button>
    </div>
  `;

  document.getElementById("audit-modal").classList.add("active");
};

window.closeModal = function() {
  document.getElementById("audit-modal").classList.remove("active");
};

// Delete application
window.deleteApplication = async function(id) {
  if (!confirm(`Delete application ${id} permanently?`)) return;
  if (isBackendAvailable) {
    try {
      await fetch(`${API_BASE}/api/history/${id}`, { method: "DELETE" });
    } catch (err) {}
  }
  portfolioRecords = portfolioRecords.filter(x => x.result.applicationId !== id);
  saveClientRecords();
  showToast("Application deleted", "info");
  refreshHistoryTable();
  refreshDashboard();
};

// Interactive What-If Simulator
function setupSimulator() {
  const sliders = [
    { id: "sim-creditScore", valId: "sim-creditScore-val", suffix: "" },
    { id: "sim-annualIncome", valId: "sim-annualIncome-val", prefix: "$", fmt: true },
    { id: "sim-existingDebt", valId: "sim-existingDebt-val", prefix: "$/mo ", fmt: true },
    { id: "sim-loanAmount", valId: "sim-loanAmount-val", prefix: "$", fmt: true },
    { id: "sim-termMonths", valId: "sim-termMonths-val", suffix: " Mos" },
    { id: "sim-liquidAssets", valId: "sim-liquidAssets-val", prefix: "$", fmt: true }
  ];

  sliders.forEach(s => {
    const input = document.getElementById(s.id);
    const label = document.getElementById(s.valId);
    if (input && label) {
      input.addEventListener("input", () => {
        let v = input.value;
        if (s.fmt) v = formatCurrency(parseFloat(v));
        label.textContent = (s.prefix || "") + v + (s.suffix || "");
        triggerSimulatorRecalculate();
      });
    }
  });

  const colType = document.getElementById("sim-collateralType");
  if (colType) {
    colType.addEventListener("change", triggerSimulatorRecalculate);
  }

  // Run initial simulator calculation
  triggerSimulatorRecalculate();
}

function triggerSimulatorRecalculate() {
  clearTimeout(simulatorDebounceTimer);
  simulatorDebounceTimer = setTimeout(runSimulatorEvaluation, 80);
}

function runSimulatorEvaluation() {
  const getNum = (id) => parseFloat(document.getElementById(id)?.value || 0);

  const payload = {
    fullName: "Simulator Applicant",
    annualIncome: getNum("sim-annualIncome"),
    existingMonthlyDebt: getNum("sim-existingDebt"),
    monthlyHousingPayment: getNum("sim-annualIncome") > 0 ? (getNum("sim-annualIncome") / 12) * 0.25 : 1000,
    liquidAssets: getNum("sim-liquidAssets"),
    creditScore: parseInt(document.getElementById("sim-creditScore")?.value || 700, 10),
    delinquenciesLast2Years: 0,
    revolvingCreditUtilizationPct: 25.0,
    creditHistoryYears: 8.0,
    loanAmount: getNum("sim-loanAmount"),
    loanPurpose: "PERSONAL",
    loanTermMonths: parseInt(document.getElementById("sim-termMonths")?.value || 36, 10),
    requestedInterestRate: 7.5,
    collateralType: document.getElementById("sim-collateralType")?.value || "NONE",
    collateralValue: getNum("sim-loanAmount") * 1.25
  };

  // Immediate synchronous calculation for instant 60fps slider response
  const r = executeUnderwritingClientEngine(payload, "SIM-ACTIVE");

  const badge = document.getElementById("sim-result-decision");
  if (badge) {
    badge.className = `status-chip ${r.decision.toLowerCase()}`;
    badge.textContent = formatDecisionLabel(r.decision);
  }

  document.getElementById("sim-result-score").textContent = Math.round(r.compositeRiskScore);
  document.getElementById("sim-result-rating").textContent = `${r.riskRating} (${r.riskRatingTier})`;
  document.getElementById("sim-result-rating").style.color = getRatingColor(r.riskRating);
  document.getElementById("sim-result-emi").textContent = "$" + formatCurrency(r.monthlyInstallment);
  document.getElementById("sim-result-dti").textContent = r.debtToIncomeRatio.toFixed(1) + "%";
  document.getElementById("sim-result-pd").textContent = (r.probabilityOfDefault * 100).toFixed(2) + "%";
  document.getElementById("sim-result-loss").textContent = "$" + formatCurrency(r.expectedLoss);
  document.getElementById("sim-result-rec-rate").textContent = r.recommendedInterestRate.toFixed(2) + "%";
  document.getElementById("sim-result-max-loan").textContent = "$" + formatCurrency(r.maximumAffordableLoan);
}

// Stress Testing
function populateStressAppDropdown() {
  const sel = document.getElementById("stress-app-select");
  if (!sel) return;
  sel.innerHTML = "";

  portfolioRecords.forEach(rec => {
    const opt = document.createElement("option");
    opt.value = rec.result.applicationId;
    opt.textContent = `${rec.result.applicationId} - ${rec.result.applicantName} ($${formatCurrency(rec.application.loanAmount)})`;
    sel.appendChild(opt);
  });
}

async function handleRunStressTest() {
  const appId = document.getElementById("stress-app-select")?.value;
  if (!appId) {
    showToast("Please select an application to stress test", "error");
    return;
  }

  const btn = document.getElementById("btn-run-stress-test");
  btn.disabled = true;
  btn.innerHTML = `<span class="pulse-dot"></span> Simulating Macroeconomic Shocks...`;

  try {
    const rec = portfolioRecords.find(x => x.result.applicationId === appId);
    if (!rec) throw new Error("Application not found");

    const app = rec.application;
    const scenarios = [
      { name: "Baseline Scenario", rateShock: 0, incomeDrop: 0, collatDrop: 0 },
      { name: "Mild Recession", rateShock: 1.5, incomeDrop: 0.05, collatDrop: 0.10 },
      { name: "Severe Stagflation", rateShock: 3.5, incomeDrop: 0.15, collatDrop: 0.25 },
      { name: "Asset Deflation Shock", rateShock: 2.5, incomeDrop: 0.10, collatDrop: 0.35 }
    ];

    const baseRes = rec.result;
    const scenarioResults = scenarios.map(sc => {
      const stressedApp = { ...app };
      stressedApp.requestedInterestRate = (app.requestedInterestRate || 7.0) + sc.rateShock;
      stressedApp.annualIncome = (app.annualIncome || 75000) * (1 - sc.incomeDrop);
      if (stressedApp.collateralValue) stressedApp.collateralValue *= (1 - sc.collatDrop);

      const stressedRes = executeUnderwritingClientEngine(stressedApp, app.id);
      const survives = stressedRes.debtToIncomeRatio <= 50 && stressedRes.decision !== "DECLINED";

      let warning = "Passes stress scenario: Borrower retains debt service headroom.";
      if (!survives) {
        if (stressedRes.debtToIncomeRatio > 50) warning = `Debt service capacity breaches ceiling: Stressed DTI reaches ${stressedRes.debtToIncomeRatio.toFixed(1)}%.`;
        else warning = `Elevated risk migration: Default probability spikes to ${(stressedRes.probabilityOfDefault * 100).toFixed(2)}%.`;
      }

      return {
        scenarioName: sc.name,
        baseMonthlyPayment: baseRes.monthlyInstallment,
        stressedMonthlyPayment: stressedRes.monthlyInstallment,
        baseDti: baseRes.debtToIncomeRatio,
        stressedDti: stressedRes.debtToIncomeRatio,
        basePd: baseRes.probabilityOfDefault,
        stressedPd: stressedRes.probabilityOfDefault,
        baseExpectedLoss: baseRes.expectedLoss,
        stressedExpectedLoss: stressedRes.expectedLoss,
        baseRating: baseRes.riskRating,
        stressedRating: stressedRes.riskRating,
        survivesStress: survives,
        failureWarning: warning
      };
    });

    renderStressTestResults({ applicantName: rec.result.applicantName, scenarios: scenarioResults });
    showToast("Stress test completed for " + rec.result.applicantName, "success");
  } catch (err) {
    showToast(err.message, "error");
  } finally {
    btn.disabled = false;
    btn.innerHTML = `<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M13 10V3L4 14h7v7l9-11h-7z"/></svg> Run CCAR / Basel Stress Test`;
  }
}

function renderStressTestResults(data) {
  const container = document.getElementById("stress-scenarios-container");
  if (!container) return;
  container.innerHTML = "";

  data.scenarios.forEach(s => {
    const div = document.createElement("div");
    div.className = `stress-card ${s.survivesStress ? 'pass' : 'fail'}`;
    div.innerHTML = `
      <div>
        <div class="stress-header">
          <div class="stress-title">${s.scenarioName}</div>
          <span class="status-chip ${s.survivesStress ? 'approved' : 'declined'}">
            ${s.survivesStress ? 'PASS' : 'BREACH'}
          </span>
        </div>
        <div class="stress-stat-row">
          <span style="color:var(--text-secondary);">Stressed Monthly EMI:</span>
          <span class="stress-stat-val">$${formatCurrency(s.stressedMonthlyPayment)}</span>
        </div>
        <div class="stress-stat-row">
          <span style="color:var(--text-secondary);">Stressed DTI:</span>
          <span class="stress-stat-val" style="color:${s.stressedDti > 45 ? '#ef4444' : 'inherit'};">${s.stressedDti.toFixed(1)}%</span>
        </div>
        <div class="stress-stat-row">
          <span style="color:var(--text-secondary);">Stressed Default Prob:</span>
          <span class="stress-stat-val" style="color:#f59e0b;">${(s.stressedPd * 100).toFixed(2)}%</span>
        </div>
        <div class="stress-stat-row">
          <span style="color:var(--text-secondary);">Expected Loss:</span>
          <span class="stress-stat-val">$${formatCurrency(s.stressedExpectedLoss)}</span>
        </div>
        <div class="stress-stat-row">
          <span style="color:var(--text-secondary);">Rating Migration:</span>
          <span class="stress-stat-val">${s.baseRating} ➔ <strong>${s.stressedRating}</strong></span>
        </div>
      </div>
      <div style="font-size:0.75rem;margin-top:0.85rem;padding-top:0.6rem;border-top:1px solid var(--border-subtle);color:${s.survivesStress ? '#34d399' : '#f87171'};">
        ${s.failureWarning}
      </div>
    `;
    container.appendChild(div);
  });
}

// Batch Execution
async function handleRunSampleBatch() {
  const btn = document.getElementById("btn-run-sample-batch");
  btn.disabled = true;
  btn.innerHTML = `<span class="pulse-dot"></span> Underwriting 5 Applications in Parallel...`;

  const batchList = [
    PRESET_PROFILES.physician,
    PRESET_PROFILES.business_owner,
    PRESET_PROFILES.tech_engineer,
    PRESET_PROFILES.freelancer,
    PRESET_PROFILES.subprime
  ];

  try {
    batchList.forEach(p => {
      const res = executeUnderwritingClientEngine(p, "LOAN-" + Math.floor(100000 + Math.random() * 900000));
      portfolioRecords.unshift({ application: { ...p, id: res.applicationId, applicant: p }, result: res });
    });
    saveClientRecords();
    showToast(`Batch completed: ${batchList.length} applications processed.`, "success");
    await refreshHistoryTable();
    await refreshDashboard();
  } catch (err) {
    showToast("Batch processing error", "error");
  } finally {
    btn.disabled = false;
    btn.innerHTML = `<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15"/></svg> Execute Institutional Portfolio Batch`;
  }
}

// Rules Configuration
function loadRules() {
  for (const [k, v] of Object.entries(currentRules)) {
    const el = document.getElementById(`rule-${k}`);
    if (el) el.value = v;
  }
}

async function handleRulesSave(e) {
  e.preventDefault();
  const getVal = (id) => parseFloat(document.getElementById(id)?.value || 0);

  currentRules = {
    minCreditScoreAutoApprove: parseInt(document.getElementById("rule-minCreditScoreAutoApprove")?.value, 10),
    minCreditScoreAcceptable: parseInt(document.getElementById("rule-minCreditScoreAcceptable")?.value, 10),
    maxDtiAutoApprove: getVal("rule-maxDtiAutoApprove"),
    maxDtiAcceptable: getVal("rule-maxDtiAcceptable"),
    maxLtvAutoApprove: getVal("rule-maxLtvAutoApprove"),
    maxLtvAcceptable: getVal("rule-maxLtvAcceptable"),
    minReservesMonths: getVal("rule-minReservesMonths"),
    minEmploymentYears: getVal("rule-minEmploymentYears"),
    benchmarkRiskFreeRate: getVal("rule-benchmarkRiskFreeRate")
  };

  if (isBackendAvailable) {
    try {
      await fetch(`${API_BASE}/api/rules`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(currentRules)
      });
    } catch (err) {}
  }

  showToast("Underwriting policy thresholds updated!", "success");
}

// Theme Toggle
function toggleTheme() {
  const current = document.documentElement.getAttribute("data-theme");
  const next = current === "light" ? "dark" : "light";
  document.documentElement.setAttribute("data-theme", next);
  showToast(`Switched to ${next} theme`, "info");
}

// Helpers
function formatCurrency(val) {
  return (val || 0).toLocaleString("en-US", { minimumFractionDigits: 2, maximumFractionDigits: 2 });
}

function formatDecisionLabel(decision) {
  const map = {
    APPROVED: "Approved",
    CONDITIONAL_APPROVAL: "Conditional Approval",
    REFER_MANUAL_REVIEW: "Refer for Manual Review",
    DECLINED: "Declined"
  };
  return map[decision] || decision;
}

function getRatingColor(r) {
  const map = {
    AAA: "#10b981", AA: "#22c55e", A: "#84cc16",
    BBB: "#eab308", BB: "#f59e0b", B: "#f97316",
    CCC: "#ef4444", D: "#991b1b"
  };
  return map[r] || "#3b82f6";
}

function showToast(msg, type = "info") {
  const container = document.getElementById("toast-container");
  if (!container) return;

  const t = document.createElement("div");
  t.className = "toast";
  t.innerHTML = `<span>${msg}</span>`;
  container.appendChild(t);

  setTimeout(() => {
    t.style.opacity = "0";
    setTimeout(() => t.remove(), 300);
  }, 3500);
}
