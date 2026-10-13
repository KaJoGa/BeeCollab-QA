# BeeCollab — Master Test Plan

| | |
|---|---|
| **Version** | 1.0 (Final Execution & Sign-Off) |
| **Date** | 2026-10-10 |
| **Application under test** | **BeeCollab** — Real-time Web Video Meeting & Collaboration Platform (Next.js 16 + React 19 + NestJS 11 + Prisma 7 + Neon PostgreSQL + Socket.io + Full Mesh WebRTC). Target live deployment at `https://beecollab.vercel.app` & `https://beecollab-rwbj.onrender.com`. |
| **Spec baseline** | [`test/BeeCollab.md`](test/BeeCollab.md) & [`test-cases/traceability.csv`](test-cases/traceability.csv) |

---

## 1. Objectives

1. Verify that **BeeCollab** behaves strictly as specified in `test/BeeCollab.md`, evaluated from the perspective of an end-user (black-box).
2. Identify, isolate, and document defects with reproducible steps, environmental context, and clear severity ratings.
3. Demonstrate structured, professional Quality Assurance practices: test analysis & design, multi-layered execution (manual + automated), API contract validation, authorization testing, exploratory testing, performance baselines, and defect lifecycle tracking.

## 2. Scope

### 2.1 In scope

**Universal Platforms, Devices & Execution Matrix**

| Tier | Platform / Device Environment | Scope & Execution Depth |
|---|---|---|
| **Primary (Desktop / Web)** | Desktop Browser (Chrome, Edge, Firefox, Safari on Windows/macOS) | Full manual exploration + automated end-to-end regression (Selenium/Playwright) |
| **Primary (Mobile Native / Web)** | Physical Mobile Device (Android over USB / iOS TestFlight) or Emulator/Simulator | Mobile responsive web or Native Mobile automation via Appium (UiAutomator2 / XCUITest); touch gestures, device lifecycles |
| **Secondary / CLI / Headless** | Target CLI / Background Microservice / Secondary OS | Functional contract validation, subprocess assertions, API endpoints |
| **Excluded / Limitations** | <EXCLUDED_PLATFORMS> (e.g. Physical iOS device if hardware unavailable) | Documented explicitly as a known coverage gap with rationale |

**Features Under Test**

| Feature Area | Spec Prefix | Key Test Scenarios & Considerations |
|---|---|---|
| Authentication & Session | `AUTH` | Sign-in, sign-out, session expiration, cancelled popup/flow, multi-user isolation |
| Navigation & App Shell | `NAV` | View switching, deep links/query params, layout boundaries, responsive breakpoints, themes |
| Core Domain / Primary Flows | `<CORE>` | Main dashboard, entity creation, real-time recalculations, list views |
| Media & Device Input | `<INPUT>` | File upload, camera/voice capture, rich text, auto-stop timeouts, permission revocation |
| Save & Feedback Workflows | `SAVE`, `FEEDBACK` | Action toasts, undo timers, modal dialogs, status alerts, error states |
| Form Input & Validation | `FORM` | Direct form inputs, character limits, client vs server validation, field formatting |
| History & Data Records | `HIST` | Filtering, grouping, cursor/offset pagination, record deletion, multi-select |
| Analytics & Summaries | `SUMMARY` | Period calculations (day/week/month), category rollups, visualization/chart consistency |
| External Integrations & Export | `EXPORT` | Third-party OAuth, API export, file generation, idempotency, retry on failure |
| Backend & API Services | `API` | Status codes, schema contracts, payload constraints, fallback chains, error sanitization |
| Domain / Processing Rules | `RULES` | Slang/token parsing, currency rules, enum matching, boundary thresholds |
| Data Security & Authorization | `SEC` | Access control rules, record ownership, unauthorized access denial, append-only logs |
| Lifecycle & Offline Resilience | `LIFE`, `OFFLINE` | **Native Mobile:** App backgrounding/resume, orientation, memory pressure, push notifications.<br>**Web/PWA:** Offline banner, cache invalidation, reconnect queue sync. |

**Test Types:** Functional, boundary/negative, session-based exploratory, cross-platform/device, API contract & data-driven, authorization/security rules, usability sanity, regression automation, and API stress/performance baseline.

### 2.2 Out of scope

- Modifying or patching application source code; white-box unit testing of internal component implementations.
- Testing SLA availability and infrastructure health of upstream third-party platforms (e.g., Google, AWS, Stripe) beyond how the app under test handles upstream failures and timeouts.
- Stress testing against live production environments or paid third-party APIs (avoids bill shock and service degradation). Stress runs are confined to local/staging with stubs.
- In-depth penetration testing and exploit crafting. Scope focuses on authorization bypass, input sanitization, formula injection, and API access controls.
- Untargeted browsers and operating systems outside the defined test matrix.
- Full accessibility (WCAG) and localization audits (sanity checks included in exploratory sessions).
- Direct testing of CI/CD deployment infrastructure.
- **Architectural Exclusions Stated Honestly:**
  - *If target uses NoSQL:* Relational SQL testing (DBeaver, PostgreSQL) is out of scope and documented as such rather than artificially fabricated.
  - *If target uses SQL:* NoSQL collection rule testing is out of scope.
  - *If target is Stateless / No DB:* Database testing in its entirety is out of scope; client persistence is tested instead.
  - *If target is Web-only:* Native mobile APK capabilities (push notifications, AVD lifecycles) are out of scope.
  - *If target is Native-only:* Browser-specific service worker and PWA manifest assertions are out of scope.

## 3. Test Approach

### 3.1 Test Layers & Technology Adaptation Matrix

Testing is organized into distinct, complementary layers designed to maximize defect detection while keeping execution fast and deterministic.

> [!IMPORTANT]
> **Universal Technology Adaptation:** Adapt the implementation of each layer to match the target project's underlying architecture:

| Layer | Focus Area | Technology Variant A (SQL Stack) | Technology Variant B (NoSQL Stack) | Technology Variant C (No Database / Stateless) |
|---|---|---|---|---|
| **L1 API Contract** | Status codes, schema, headers, sanitization | REST/JSON via Postman/Newman | REST/JSON via Postman/Newman | REST, GraphQL, or Client Module contract |
| **L2 Data Quality** | Data processing, parsing, business logic | Data-driven Postman (JSON/CSV) | Data-driven Postman (JSON/CSV) | Unit / Data-driven runner with fixtures |
| **L3 Data & Security** | Data integrity, access control, isolation | **SQL Schema & Access:** Dockerized DB, foreign key cascades, constraints, RLS policies, DBeaver validation queries | **NoSQL Security Rules:** Local emulator, document validation, client-side allow/deny rules, tenant isolation | **State & Persistence:** LocalStorage, IndexedDB, Session Cache, state transitions, HTTP cache headers (`no-store`) |
| **L4 UI / Device E2E** | User journeys, screens & views | **Web:** Selenium / Playwright + Cucumber BDD + Java/Maven | **Web:** Selenium / Playwright + Cucumber BDD + Java/Maven | **Native Mobile:** Appium (UiAutomator2 / XCUITest) + Cucumber BDD |
| **L4 Mobile Automation** | Mobile responsive / native gestures | Appium (UiAutomator2 / XCUITest) | Appium (UiAutomator2 / XCUITest) | Appium (UiAutomator2 / XCUITest) |
| **L5 Complex Integrations** | External OAuth, file sync, exports | Structured manual run protocol | Structured manual run protocol | Structured manual run protocol |
| **Stress & Performance** | Concurrency ramp & failure resilience | k6 (local dev + mock upstream) | k6 (local dev + mock upstream) | k6 or in-memory execution benchmarking |
| **Exploratory Smoke** | Adversarial input, UX, edge cases | Session-Based Test Management (SBTM, 5–6 charters) | Session-Based Test Management (SBTM, 5–6 charters) | Session-Based Test Management (SBTM, 5–6 charters) |

### 3.2 Manual Testing

- Every manual test case is authored from the functional specification and stored as structured **CSV** files in `test-cases/` (schema defined in §7.1).
- Session-based exploratory testing (SBTM) is conducted using dedicated charters in `test-cases/exploratory/`, specifically probing complex risk areas (see §4).
- Execution results (Pass / Fail / Blocked / Retest) are logged in `test-cases/traceability.csv`, recording build version, date, and defect links.

### 3.3 UI & Device Automation (L4)

- Scenarios are written in Gherkin syntax and tagged with specific spec IDs (`@HIST-08`, `@smoke`, `@regression`, `@manual-only`).
- **Tag Tiers:**
  - `@smoke`: Fast gate suite verifying critical user paths (runs in minutes).
  - `@regression`: Comprehensive regression suite covering edge cases and full journeys.
  - `@manual-only`: Documented scenarios intentionally excluded from automation (e.g. hardware permissions, native OS dialogues).
- **Execution Standards:**
  - Strict Page Object Model (POM) architecture isolating locators and page actions from step definitions.
  - Explicit waits only — zero arbitrary sleeps (`Thread.sleep`).
  - Strict scenario isolation: test data is seeded and cleaned per scenario; never rely on shared persistent state.
  - For Web: Chromium flags for virtual media streams (`--use-fake-device-for-media-stream`).
  - For Native Mobile: Appium capabilities configuring `appPackage`, `appActivity`, `automationName: UiAutomator2`, resetting app state between test fixtures.

### 3.4 API Testing (L1 & L2)

- Target local or isolated staging instances to prevent dirtying production data.
- **Test Scopes:**
  - Positive contract tests: HTTP 200/201, schema conformity, required fields, enum validation.
  - Negative/Edge tests: HTTP 400/422, invalid payload types, whitespace-only strings, oversized payloads, boundary values.
  - Resilience/Fallback tests: Simulate upstream 429/5xx errors using local stubs to verify the fallback chain.
  - Security/Caching headers: Validate `Cache-Control`, `X-Content-Type-Options`, and auth enforcement.

### 3.5 Non-Determinism & Third-Party Fallback Strategy

When testing probabilistic components (e.g. AI/LLM parsers, external recommendation engines):
- Assert **contracts and invariants**, not freeform text: JSON validity, required keys present, valid ranges for numeric fields, enum compliance.
- Assert mandatory business values strictly; apply tolerant matching (case-insensitivity, whitespace trimming) to optional notes or freeform descriptions.
- Run quality datasets multiple times (e.g. 5 iterations); calculate accuracy percentage and flag intermittent flakiness.
- Deterministic app gates (e.g. reject below threshold) must be tested against deterministic **stubbed responses**.

### 3.6 Performance & Stress Testing

- Executed using lightweight, reproducible **k6** scripts targeting local or staging endpoints with upstream services stubbed.
- **Scenarios:**
  - Ramp-up test: 1 -> 20 Virtual Users (VUs) over 1–2 minutes to measure throughput and baseline latency.
  - Edge-case / burst test: Sustained concurrency while triggering error-handling or fallback logic.
- **Key Metrics:** Request rate (req/s), error rate (%), p50 / p90 / p95 latency. Results stored as a permanent baseline in `performance/results/`.

### 3.7 Defect Management & Tracking

- Defects are logged in an external issue tracker (e.g. **Jira Cloud**).
- This repository maintains thin markdown stubs in `bug-reports/BUG-###-<slug>.md` mirroring essential information (ID, title, spec ID, severity, steps, expected vs. actual, and Jira URL) so the repository remains fully auditable offline.
- **Severity Classification:**
  - **Critical:** Data corruption/loss, total system crash, critical security breach, login/save completely blocked.
  - **High:** Major business feature broken with no feasible workaround.
  - **Medium:** Secondary feature failure, or major failure with an acceptable workaround.
  - **Low:** Cosmetic defect, minor UI alignment issue, spelling error, subtle usability friction.
- **Scrum Workflow:** Testing operates within 2–3 short sprints reflecting real sprint cycles (Sprint 1: Test design & baseline execution; Sprint 2: Developer fixes; Sprint 3: Verification, regression, new features).

## 4. Universal Risk Analysis Matrix

Ranked by *Likelihood of Defect × Business Impact*. Use this ranking to prioritize manual exploratory sessions and regression depth:

| # | Risk Area | Why It Is Likely to Break | What to Probe | Traced Spec IDs |
|---|---|---|---|---|
| **R1** | **Core Data Processing & Schema Validation** (High) | Complex parsing, type conversions, and model integrations frequently produce unexpected data types or silent failures. | Missing mandatory fields, zero values, extreme amounts, unmapped enums, invalid types. | `<RULES-*>`, `<API-*>` |
| **R2** | **Adversarial & Boundary Input Handling** (High) | User-supplied strings reach databases, export files, and external APIs without adequate sanitization. | Script tags (`<script>`), prompt injection strings, CSV/spreadsheet formula injections (`=HYPERLINK`), extreme string lengths (>500 chars), RTL / unicode text. | `<FORM-*>`, `<EXPORT-*>` |
| **R3** | **Service Failure & Fallback Resilience** (High) | Downstream APIs (gateways, AI engines, auth providers) suffer latency spikes, rate limits (429), or 5xx outages. | Primary service timeout, fallback service triggering, total failure messaging (friendly vs raw leaks), recovery upon reconnect. | `<API-FALLBACK>`, `<RESILIENCE>` |
| **R4** | **Hardware, Peripherals & Device Permissions** (High) | Mobile OS / Browser security models revoke permissions dynamically; hardware state changes mid-session. | Permission denied/revoked mid-session, recording timeouts, app backgrounded during active capture, silent hardware access failure. | `<MEDIA-*>`, `<PERM-*>` |
| **R5** | **Third-Party Export & Synchronization** (High) | Multi-step external API calls (create file, populate sheets, mark synced) are non-atomic and prone to network drops. | Mid-sync network disconnection, token expiry, repeated sync without duplicates (idempotency), external file deleted by user, midnight/timezone date mapping. | `<EXPORT-*>`, `<SYNC-*>` |
| **R6** | **Time-Zone & Date-Boundary Calculations** (Medium-High) | Aggregations (Today, This Week, This Month) depend on device local time while servers use UTC. | Transactions recorded at 23:59 vs 00:00, month-end rollups, leap years, mismatched device time zones. | `<TIME-*>`, `<SUMMARY-*>` |
| **R7** | **Pagination × Filtering × Real-Time Updates** (Medium-High) | Combining offset/cursor pagination with active filters and real-time socket updates produces phantom rows or missing items. | Page size boundaries (exact page size + 1), filter change resetting page index to 1, deleting items on page N, concurrent inserts while browsing older pages. | `<HIST-*>` |
| **R8** | **Lifecycle, Offline Persistence & State Recovery** (Medium-High) | Network transitions and OS state interruptions frequently drop queued state or corrupt local caches. | **Native Mobile:** App backgrounding, incoming call interruption, low-memory kill, app resume.<br>**Web/PWA:** Offline banner, cache invalidation, reconnect sync. | `<LIFE-*>`, `<OFFLINE-*>` |
| **R9** | **Authorization & Multi-Tenant Data Isolation** (Medium / Impact: Critical) | Client-side validation without strict server-side rules allows unauthorized reads/writes via direct API/DB calls. | Cross-user data access, tampering with user IDs in requests, unauthenticated queries, modifying append-only audit logs. | `<SEC-*>`, `<AUTH-*>` |
| **R10** | **Rate Limiting & Endpoint Cost Abuse** (Medium) | Unauthenticated or unthrottled endpoints can be flooded, exhausting cloud quotas or racking up infrastructure costs. | High-frequency calls, burst submissions, payload flooding up to body limit. | `<API-LIMITS>` |
| **R11** | **State Collisions across Edit / Undo / Delete** (Medium) | Optimistic UI updates colliding with async confirmation dialogs and undo toast timers. | Rapid clicking of delete/save, undo after toast timeout, navigating away while modal is confirming. | `<SAVE-*>`, `<DIALOG-*>` |
| **R12** | **Cross-Device UI Consistency & Responsive Polish** (Low-Medium) | Responsive layouts breaking on narrow screens, mobile virtual keyboard covering action buttons. | Viewport resizing, orientation changes, virtual keyboard opening over submit buttons, dark/light theme switching. | `<NAV-*>`, `<LAYOUT-*>` |

## 5. Entry & Exit Criteria

### 5.1 Entry Criteria (Starting a Test Cycle)
- Functional Specification (`test/BeeCollab.md`) is baselined and version-controlled.
- Target build / deployment identifier is stable and accessible.
- Smoke sanity check passes (successful sign-in, create entity, view dashboard).
- Test environments configured: emulators booted, test stubs active, test credentials verified.
- Test cases authored, peer-reviewed, and traceability links established.

### 5.2 Exit Criteria (Closing a Test Cycle)
- **Execution Depth:** 100% of Priority 1 (P1 - Smoke/Critical) test cases executed; ≥ 95% of total planned cases executed (unexecuted cases documented with justifications).
- **Pass Rate:** 100% pass rate on P1 cases; ≥ 90% overall pass rate on executed cases.
- **Defect Thresholds:** **Zero open Critical or High** defects. All open Medium defects have documented triage decisions (Fix, Defer, or Accepted Risk).
- **Regression Suite:** Automated smoke and regression suites execute green on target CI/environment across multiple consecutive runs.
- **Traceability:** 100% of functional spec IDs are covered in `test-cases/traceability.csv`.
- **Reporting:** Formal Cycle Summary Report completed and signed off in `reports/`.

### 5.3 Suspension & Resumption Criteria
- **Suspend Testing:** If application build is unreachable, core authentication is broken, test emulator/database is unavailable, or critical blocker impedes >50% of test paths.
- **Resume Testing:** Upon deployment of a verified hotfix build, resolution of environment outage, and successful re-execution of the smoke suite.

## 6. Environment Strategy & Tooling Decisions

**Core Principle:** Automated, bulk, and destructive tests must never run against production environments or touch real customer data. They run against dedicated local emulators, containerized instances, or isolated staging servers.

| Component | Target Environment | Strategy / Wiring |
|---|---|---|
| **Authentication Service** | Local Auth Emulator / Mock Provider | Synthetic accounts created on-the-fly; no real external credentials needed in automation |
| **Primary Database (SQL / NoSQL / None)** | Local DB Container / Emulator / Storage | Dedicated test instance with automated per-scenario reset and seeding scripts |
| **Third-Party APIs / External Services** | Local Network Preload Stubs (`fetch`/HTTP mock) | Intercept outbound HTTP requests; canned deterministic responses for high, low, and failure scenarios |
| **Application Server / Mobile Build** | Local dev build or test APK/IPA build | Run on isolated port or sideloaded onto connected test device |
| **Mobile & Real Devices** | Physical Device / BrowserStack / Appium | Connected via USB debugging or local Wi-Fi; targeted at staging or production smoke |

### 6.1 Test Data & Fixture Strategy
- **Synthetic Users:** Create at least two distinct user personas (User A, User B) to systematically verify tenant isolation (`SEC-*`).
- **Data Seeding:** Seed realistic datasets (e.g. 100+ records) to validate pagination, sorting, and summary calculations under volume.
- **Boundary Fixtures:** Clean strings, strings at maximum character limit, strings containing special characters / formulas, empty payloads, oversized payloads.

### 6.2 Tooling Decisions & Data Layer Adaptation Rationale
- **If SQL Stack:** Use JDBC/Node test runners and DBeaver to execute SQL test scripts validating schema constraints, foreign key cascades, and row-level security.
- **If NoSQL Stack:** Use native rules unit testing tools (e.g. `@firebase/rules-unit-testing`) to execute client-side security policies against local emulators.
- **If Stateless / No DB:** Use browser storage inspection utilities and HTTP client assertions to validate LocalStorage/IndexedDB state persistence and HTTP cache control headers.

## 7. Deliverables

| Deliverable | Repository Path | Description |
|---|---|---|
| Master Test Plan | `test-plan.md` | Test strategy, scope, risks, and governance |
| Functional Specification | `test/BeeCollab.md` | Single source of truth for black-box requirements |
| Test Cases (CSV) | `test-cases/*.csv` | Structured test cases organized by feature area |
| Traceability Matrix | `test-cases/traceability.csv` | Spec ID to Test Case ID mapping with execution results |
| Exploratory Charters | `test-cases/exploratory/` | Session-based charters and completed execution notes |
| Manual Run Guides | `test-cases/*-run-guide.md` | Step-by-step procedures for complex manual flows |
| UI & Mobile Automation Suite | `automation/` | BDD Page Object automation suite (Java/Selenium/Playwright/Appium) |
| API Test Collections | `api-testing/` | Postman collections and Newman execution scripts |
| Security & Data Suite | `security-rules/` | Access control, SQL constraint, or state persistence scripts |
| Performance Baselines | `performance/` | k6 load scripts and baseline performance metrics |
| Defect Reports | `bug-reports/` | Individual bug report stubs and master `INDEX.md` |
| Test Summary Reports | `reports/` | Cycle summaries and final executive summary |

### 7.1 Test Case CSV Schema Specification

Individual feature CSV files (`test-cases/<area>.csv`) adhere to this standard 10-column schema:

```csv
case_id,spec_id,title,preconditions,steps,expected_result,priority,type,environment,automated
```

- `case_id`: Unique identifier (e.g. `TC-HIST-001`, `TC-AUTH-002`).
- `spec_id`: Semicolon-separated spec IDs covered (e.g. `HIST-08;HIST-09`).
- `title`: Concise summary of what is being tested.
- `preconditions`: Required state before execution starts.
- `steps`: Numbered, reproducible test actions separated by line breaks or semicolons.
- `expected_result`: Expected system behavior per the functional spec.
- `priority`: `P1` (Smoke/Critical), `P2` (Major), `P3` (Minor).
- `type`: `functional`, `negative`, `boundary`, `security`, or `exploratory-charter`.
- `environment`: `local-emulator`, `staging`, or `production`.
- `automated`: `Y`, `N`, or `planned`.

## 8. Repository Structure

```
QA-BeeCollab/
├── test-plan.md                          # Master Test Plan (this document)
├── README.md                             # Portfolio landing page & executive summary
├── test/                                 # Functional specification baseline & test tools
│   ├── BeeCollab.md                      # Single source of truth functional spec (IDs: AUTH, MEET, etc.)
│   ├── README.md                         # Testing strategy, ground rules & layer overview
│   └── tools/                            # Node.js API & WebSocket test scripts
├── test-cases/                           # Structured test cases & execution records
│   ├── README.md                         # CSV format reference, naming rules & conventions
│   ├── traceability.csv                  # Master traceability: Spec ID -> Case ID -> Layer -> Result
│   ├── <area-1>.csv                      # Feature area test cases (e.g. auth-nav.csv)
│   ├── <area-2>.csv                      # Feature area test cases (e.g. core-transactions.csv)
│   ├── <integration>-run-guide.md        # Manual protocol guide for complex third-party flows
│   └── exploratory/                      # Session-Based Test Management (SBTM)
│       ├── README.md                     # Charter index & exploratory guidelines
│       └── CH-01-<slug>.md               # Charter: Mission, test ideas & completed session notes
├── automation/                           # L4 UI / Mobile end-to-end automation suite
│   ├── pom.xml                           # Build configuration (Java 17, Selenium/Appium, Cucumber)
│   └── src/test/
│       ├── java/.../{pages,steps,hooks,support,runners}
│       └── resources/{features,fixtures,config}
├── api-testing/                          # L1 & L2 API testing suite
│   ├── README.md                         # Newman/Postman execution guide
│   ├── collections/                      # Contract & data-driven JSON collections
│   ├── environments/                     # *.example.json environment configs (committed)
│   └── data/                             # Data-driven CSV/JSON test payloads
├── security-rules/                       # L3 Access control, SQL integrity, or state persistence
│   ├── package.json / build file
│   └── README.md                         # Layer 3 execution guide (SQL / NoSQL / No DB)
├── performance/                          # Performance & stress testing
│   ├── README.md                         # k6 execution guide & test scenarios
│   └── results/                          # Baseline latency and throughput markdown reports
├── environments/                         # Test environment harnesses & mock utilities
│   ├── README.md                         # Environment setup notes
│   ├── emulator/                         # Emulator configs & startup scripts
│   └── stubs/                            # Outbound request preload interceptors & stubs
├── bug-reports/                          # Defect tracking & audit trail
│   ├── README.md                         # Defect lifecycle & reporting guidelines
│   ├── INDEX.md                          # Master defect index table
│   ├── TEMPLATE.md                       # Bug report stub markdown template
│   └── BUG-###-<slug>.md                 # Individual defect stubs linking to Jira
└── reports/                              # Test summary reports & sign-offs
    ├── README.md                         # Reporting cadence & templates
    ├── cycle-summary-template.md         # Per-cycle / sprint test summary report
    └── final-summary-template.md         # Executive one-page final project summary
```
