# Credit Risk Evaluation & Underwriting System (Java 25)

An enterprise-grade, institutional **Credit Risk Evaluation & Underwriting Engine** built in **pure Java 25**. Features quantitative multi-pillar credit scoring, Basel II/III regulatory risk rating tiers, calibrated Probability of Default (PD), Loss Given Default (LGD), Expected Loss (EL), CCAR macroeconomic stress testing, portfolio Value at Risk (VaR), an interactive FinTech web dashboard, and a command-line interface (CLI).

---

## Key Capabilities & Risk Methodology

### 1. Multi-Pillar Quantitative Credit Scorecard (0 - 1000)
Evaluates creditworthiness across the five fundamental pillars of credit underwriting:
- **Pillar 1: Credit Bureau & Repayment Track (35% weight)**: FICO/Bureau score, 24-month delinquency marks, revolving credit utilization, hard inquiries, public records/liens.
- **Pillar 2: Capacity & Leverage Ratios (25% weight)**: Debt-to-Income (DTI %), Payment-to-Income (PTI %), and disposable income headroom.
- **Pillar 3: Capital & Liquidity Reserves (15% weight)**: Months of payment coverage in liquid cash/savings and liquid-to-loan ratios.
- **Pillar 4: Collateral Coverage & Quality (15% weight)**: Real estate, cash deposits, marketable securities, vehicles, equipment, adjusted by haircut liquidation values and LTV %.
- **Pillar 5: Employment & Stability Conditions (10% weight)**: Employment stability factor (Salaried, Business Owner, Freelancer), tenure length, and credit history depth.

### 2. Regulatory & Mathematical Risk Models
- **Probability of Default (PD %)**: Calibrated logistic sigmoid curve mapping composite score (0-1000) to 1-year empirical default probability ($0.15\%$ for prime borrowers up to $85\%$ for distressed subprime).
- **Loss Given Default (LGD %)**: Collateral liquidation haircuts (Real estate: 20% LGD, Cash: 5% LGD, Vehicles: 35% LGD, Unsecured: 80% LGD) dynamically scaled by Loan-to-Value (LTV).
- **Exposure at Default (EAD $)**: Loan principal plus recovery and accrued interest factor.
- **Expected Loss (EL $)**:
  $$\text{EL} = \text{PD} \times \text{LGD} \times \text{EAD}$$
- **Portfolio Value at Risk (VaR)**: Parametric 95% and 99% confidence level loss distribution thresholds.
- **Basel II/III Rating Tiers**: Automated mapping to `AAA`, `AA`, `A`, `BBB`, `BB`, `B`, `CCC`, and `D`.

### 3. Automated Decision Engine
- **Decisions**: `APPROVED`, `CONDITIONAL_APPROVAL`, `REFER_MANUAL_REVIEW`, `DECLINED`.
- **Hard Knockout Rules**: Bankruptcy/public records within 2 years, past default with sub-620 FICO, DTI exceeding policy ceiling ($> 50\%$).
- **Adverse Action Reason Codes**: Auto-generated FCRA-compliant risk factors.
- **Underwriting Stipulations**: Tailored mitigating conditions (e.g., additional down payment to lower LTV $\le 80\%$, debt pay-down to reduce DTI $\le 36\%$, co-signer requirement).
- **Risk-Based Pricing Model**: Base risk-free benchmark + rating tier credit spread.
- **Maximum Affordable Loan Calculator**: Amortization-based borrowing limit.

### 4. CCAR / Basel Stress Testing Suite
Simulates adverse macroeconomic shocks:
- **Baseline**: Current economic climate.
- **Mild Recession**: $+150$ bps interest rate hike, $-10\%$ collateral valuation, $-5\%$ borrower income.
- **Severe Stagflation**: $+350$ bps rate hike, $-25\%$ collateral devaluation, $-15\%$ borrower income.
- **Asset Deflation**: $-35\%$ property valuation shock, $+250$ bps rate hike.
- Tests debt service coverage ratio survival and rating migration.

---

## Project Structure

```
Credit Risk Evaluation System/
├── src/
│   └── com/creditrisk/
│       ├── Main.java                     # System entry point (CLI/Web/Eval flags)
│       ├── model/                        # Domain models
│       │   ├── Applicant.java            # Borrower profile & credit bureau data
│       │   ├── LoanApplication.java      # Loan terms, purpose, collateral
│       │   ├── RiskAssessmentResult.java # Detailed underwriting audit memo
│       │   ├── RiskDecision.java         # Approved, Conditional, Refer, Declined
│       │   ├── RiskRating.java           # Basel II/III tiers (AAA to D)
│       │   ├── LoanPurpose.java          # Mortgage, Auto, Business, etc.
│       │   ├── CollateralType.java       # Real estate, cash, vehicle, unsecured
│       │   ├── EmploymentType.java       # Salaried, Self-Employed, Freelance
│       │   ├── UnderwritingRules.java    # Configurable risk policy parameters
│       │   ├── StressTestScenario.java   # CCAR shock parameters
│       │   └── StressTestResult.java     # Stressed EMI, DTI, PD, and survival
│       ├── engine/                       # Core mathematical calculation engines
│       │   ├── CreditScoringEngine.java  # 5-Pillar scorecard, PD, LGD, EL formulas
│       │   ├── DecisionEngine.java       # Policy execution, knockouts, stipulations
│       │   └── StressTestingEngine.java  # Macroeconomic shock simulation
│       ├── repository/
│       │   └── ApplicationRepository.java# Persistence, portfolio analytics & demo data
│       ├── server/
│       │   └── CreditRiskHttpServer.java # Built-in HTTP server & REST API
│       ├── cli/
│       │   └── CreditRiskCli.java        # Interactive console evaluation tool
│       └── util/
│           └── JsonUtil.java             # Zero-dependency JSON parser & serializer
├── web/                                  # Modern Glassmorphic Web Dashboard
│   ├── index.html                        # Semantic, responsive layout
│   ├── css/style.css                     # Premium Dark/Light theme, gauges, cards
│   └── js/app.js                         # REST client, real-time simulator, charts
├── data/                                 # Persistent JSON records & rules
├── CreditRiskSystem.jar                  # Standalone executable JAR
├── run-server.bat                        # Double-click launcher for Web Server
├── run-cli.bat                           # Double-click launcher for CLI
└── build.bat                             # One-click recompilation script
```

---

## Quick Start & Running

### Option 1: Web Dashboard (Recommended)
Double-click `run-server.bat` or run:
```bash
java -jar CreditRiskSystem.jar --port 8080
```
Open your browser to: **[http://localhost:8080](http://localhost:8080)**

Features on the Web Dashboard:
- **Overview Tab**: Live portfolio exposure, expected loss, Basel rating distribution bar chart, approval breakdown, and 95%/99% VaR.
- **Evaluate Loan Tab**: Underwriting form with 1-click test profiles (Dr. Eleanor Vance - Prime Physician, Marcus Brody - Business Owner, Sophia Chen - Tech Engineer, Jackson Reed - Freelance Refer, Tyler Hayes - Subprime Declined). Displays glowing decision banner, SVG score gauge, 5-pillar bars, adverse factors, and formal memo.
- **What-If Simulator Tab**: Interactive sliders for Credit Score, Income, Debt, Loan Amount, and Collateral with instant live risk recalculation.
- **Audit Log Tab**: Searchable, filterable portfolio repository with modal memo view, deletion, and CSV export.
- **Stress Testing Tab**: Select any loan and run multi-scenario CCAR shocks.
- **Policy Rules Tab**: Adjust minimum credit score, maximum DTI/LTV, and base rates.

---

### Option 2: Interactive Terminal CLI
Double-click `run-cli.bat` or run:
```bash
java -jar CreditRiskSystem.jar --cli
```
Options available in CLI:
1. Evaluate New Loan Application (Step-by-step interactive prompt)
2. Run CCAR / Basel Stress Test
3. View Evaluated Applications & History
4. View Portfolio Analytics & Value at Risk (VaR)
5. View Institutional Underwriting Rules
6. Reset & Reload Sample Cases

---

### Option 3: Command-Line Single File Evaluation
Evaluate any JSON file directly:
```bash
java -jar CreditRiskSystem.jar --eval data/test_eval.json
```

---

## REST API Reference

| Method | Endpoint | Description |
| :--- | :--- | :--- |
| `GET` | `/api/health` | System health, uptime, and record count |
| `POST` | `/api/evaluate` | Evaluates a single loan application |
| `POST` | `/api/batch-evaluate` | Evaluates a list of loan applications |
| `GET` | `/api/history` | Retrieves all evaluated loan records |
| `GET` | `/api/history/{id}` | Retrieves a single loan evaluation record |
| `DELETE` | `/api/history/{id}` | Deletes a loan record |
| `POST` | `/api/stress-test` | Runs multi-scenario macroeconomic stress test |
| `GET` | `/api/analytics` | Portfolio exposure, default rates, and VaR |
| `GET` | `/api/rules` | Retrieves current underwriting rules |
| `POST` | `/api/rules` | Updates underwriting thresholds |
| `GET` | `/api/export` | Exports records to CSV format |

---

## Compilation

To rebuild the project:
```bash
build.bat
```
or manually:
```bash
javac -d bin src/com/creditrisk/*.java src/com/creditrisk/model/*.java src/com/creditrisk/engine/*.java src/com/creditrisk/repository/*.java src/com/creditrisk/server/*.java src/com/creditrisk/cli/*.java src/com/creditrisk/util/*.java
jar cfm CreditRiskSystem.jar manifest.txt -C bin .
```

*Built with pure Java 25 standard library. Zero external dependencies required.*
