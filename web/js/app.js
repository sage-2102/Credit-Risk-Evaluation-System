/**
 * AURA Credit Risk Evaluation System - Frontend Controller
 * Powered by Java 25 Core Engine
 */

const API_BASE = window.location.origin;

// State management
let portfolioRecords = [];
let portfolioAnalytics = {};
let currentRules = {};
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
document.addEventListener("DOMContentLoaded", () => {
  setupNavigation();
  setupEventListeners();
  loadInitialData();
  setupSimulator();
  checkServerHealth();
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
  try {
    const res = await fetch(`${API_BASE}/api/health`);
    if (res.ok) {
      const data = await res.json();
      const badge = document.getElementById("server-status-badge");
      if (badge) {
        badge.innerHTML = `<span class="pulse-dot"></span> Java 25 Engine Online`;
        badge.style.display = "inline-flex";
      }
    }
  } catch (err) {
    const badge = document.getElementById("server-status-badge");
    if (badge) {
      badge.innerHTML = `<span style="width:8px;height:8px;border-radius:50%;background:#ef4444;display:inline-block;"></span> Server Offline`;
      badge.style.borderColor = "rgba(239, 68, 68, 0.4)";
      badge.style.color = "#f87171";
    }
  }
}

// Initial Data Load
async function loadInitialData() {
  await Promise.all([refreshDashboard(), refreshHistoryTable(), loadRules()]);
  // Populate first preset by default
  fillEvaluationForm(PRESET_PROFILES.physician);
}

// Refresh Dashboard Analytics
async function refreshDashboard() {
  try {
    const res = await fetch(`${API_BASE}/api/analytics`);
    if (!res.ok) return;
    portfolioAnalytics = await res.json();
    renderDashboardMetrics(portfolioAnalytics);
  } catch (err) {
    console.error("Failed to load analytics:", err);
  }
}

function renderDashboardMetrics(data) {
  // Metric cards
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
  const getVal = (id, def = 0) => {
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
    const res = await fetch(`${API_BASE}/api/evaluate`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(payload)
    });

    if (!res.ok) {
      const err = await res.json();
      throw new Error(err.error || "Evaluation failed");
    }

    const data = await res.json();
    currentEvaluationResult = data;
    renderEvaluationResults(data.result, data.application);
    showToast(`Loan ${data.result.decision} - Score: ${data.result.compositeRiskScore}`, "success");
    
    // Refresh history and analytics in background
    refreshDashboard();
    refreshHistoryTable();

    // Scroll to results
    document.getElementById("evaluation-result-section").scrollIntoView({ behavior: "smooth" });
  } catch (err) {
    showToast(err.message, "error");
  } finally {
    btn.disabled = false;
    btn.innerHTML = `<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z"/></svg> Execute Underwriting Assessment`;
  }
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
    // 0 -> -90 deg, 1000 -> +90 deg
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
  try {
    const res = await fetch(`${API_BASE}/api/history`);
    if (!res.ok) return;
    portfolioRecords = await res.json();
    renderHistoryRows(portfolioRecords);
    populateStressAppDropdown();
  } catch (err) {
    console.error("Failed to load history:", err);
  }
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
    const a = rec.application.applicant;
    const tr = document.createElement("tr");
    tr.innerHTML = `
      <td><strong style="color:var(--accent-cyan);">${r.applicationId}</strong></td>
      <td>
        <div><strong>${r.applicantName}</strong></div>
        <div style="font-size:0.75rem;color:var(--text-muted);">${a ? a.jobTitle : ''}</div>
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
  const a = app.applicant;

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
        <div style="font-size:0.75rem;color:var(--text-muted);">${a ? a.jobTitle : ''} (${a ? a.yearsEmployed : ''} yrs)</div>
      </div>
      <div class="metric-card" style="padding:0.85rem;">
        <div class="metric-label">Requested Loan</div>
        <div style="font-size:1.1rem;font-weight:700;color:var(--accent-cyan);">$${formatCurrency(app.loanAmount)}</div>
        <div style="font-size:0.75rem;color:var(--text-muted);">${app.loanPurpose} • ${app.loanTermMonths} Mos @ ${app.requestedInterestRate}%</div>
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
  try {
    const res = await fetch(`${API_BASE}/api/history/${id}`, { method: "DELETE" });
    if (res.ok) {
      showToast("Application deleted", "info");
      refreshHistoryTable();
      refreshDashboard();
    }
  } catch (err) {
    showToast("Failed to delete", "error");
  }
};

// Interactive What-If Simulator
function setupSimulator() {
  const sliders = [
    { id: "sim-creditScore", valId: "sim-creditScore-val", suffix: "" },
    { id: "sim-annualIncome", valId: "sim-annualIncome-val", prefix: "$", fmt: true },
    { id: "sim-existingDebt", valId: "sim-existingDebt-val", prefix: "$/mo", fmt: true },
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
  simulatorDebounceTimer = setTimeout(runSimulatorEvaluation, 120);
}

async function runSimulatorEvaluation() {
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

  try {
    const res = await fetch(`${API_BASE}/api/evaluate`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(payload)
    });

    if (res.ok) {
      const data = await res.json();
      const r = data.result;

      // Update simulator live cards
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
  } catch (err) {
    console.error("Simulator error:", err);
  }
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
    const res = await fetch(`${API_BASE}/api/stress-test`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ applicationId: appId })
    });

    if (!res.ok) throw new Error("Stress test failed");
    const data = await res.json();
    renderStressTestResults(data);
    showToast("Stress test completed for " + data.applicantName, "success");
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
  btn.innerHTML = `<span class="pulse-dot"></span> Underwriting 8 Loan Applications in Parallel...`;

  const batchList = [
    PRESET_PROFILES.physician,
    PRESET_PROFILES.business_owner,
    PRESET_PROFILES.tech_engineer,
    PRESET_PROFILES.freelancer,
    PRESET_PROFILES.subprime
  ];

  try {
    const res = await fetch(`${API_BASE}/api/batch-evaluate`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(batchList)
    });

    if (res.ok) {
      const data = await res.json();
      showToast(`Batch completed: ${data.count} applications processed.`, "success");
      await refreshHistoryTable();
      await refreshDashboard();
    }
  } catch (err) {
    showToast("Batch processing error", "error");
  } finally {
    btn.disabled = false;
    btn.innerHTML = `<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15"/></svg> Execute Institutional Portfolio Batch`;
  }
}

// Rules Configuration
async function loadRules() {
  try {
    const res = await fetch(`${API_BASE}/api/rules`);
    if (!res.ok) return;
    currentRules = await res.json();
    for (const [k, v] of Object.entries(currentRules)) {
      const el = document.getElementById(`rule-${k}`);
      if (el) el.value = v;
    }
  } catch (err) {
    console.error("Failed to load rules:", err);
  }
}

async function handleRulesSave(e) {
  e.preventDefault();
  const getVal = (id) => parseFloat(document.getElementById(id)?.value || 0);

  const payload = {
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

  try {
    const res = await fetch(`${API_BASE}/api/rules`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(payload)
    });
    if (res.ok) {
      showToast("Underwriting policy thresholds updated!", "success");
    }
  } catch (err) {
    showToast("Failed to save rules", "error");
  }
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
