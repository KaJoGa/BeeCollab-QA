# BeeCollab — QA Engineering & Test Automation Portfolio

[![Java 21](https://img.shields.io/badge/Java-21%20LTS-orange.svg)](https://www.oracle.com/java/)
[![Selenium 4](https://img.shields.io/badge/Selenium-4.28.1-green.svg)](https://www.selenium.dev/)
[![Cucumber BDD](https://img.shields.io/badge/Cucumber-7.21.1-brightgreen.svg)](https://cucumber.io/)
[![JUnit 5](https://img.shields.io/badge/JUnit-5.11.4-blue.svg)](https://junit.org/junit5/)
[![Maven](https://img.shields.io/badge/Maven-3.9.x-red.svg)](https://maven.apache.org/)
[![QA Automation CI](https://github.com/KaJoGa/BeeCollab-QA/actions/workflows/qa-automation.yml/badge.svg)](https://github.com/KaJoGa/BeeCollab-QA/actions/workflows/qa-automation.yml)
[![License](https://img.shields.io/badge/License-MIT-lightgrey.svg)](LICENSE)

An enterprise-grade, multi-layer Quality Assurance and Test Automation repository for **BeeCollab** — a real-time web video conferencing and collaboration platform.

Tested directly against the **Live Production Deployment**:
* Web Frontend: [https://beecollab.vercel.app](https://beecollab.vercel.app) (Next.js 16 + React 19 App Router)
* Backend API & Gateway: [https://beecollab-rwbj.onrender.com](https://beecollab-rwbj.onrender.com) (NestJS 11 + Prisma 7 + Neon PostgreSQL + Socket.io `/meetings`)

---

## 1. Testing Strategy & Architecture Layers

This project strictly follows **black-box functional testing** against documented specifications without modifying application source code:

| Layer | Scope & Focus Area | Technology Stack | Status |
|---|---|---|---|
| **L1 REST API & Security** | DTO contract validation, IDOR unauthorized data leaks, JWT integrity (alg:none, tampered signatures), and boundary input fuzzing. | **Java 21 REST Assured** (`automation/`) & Node.js harness (`test/tools/`) | **13/13 PASS (Java) / 26/26 PASS** |
| **L3 WebSocket Real-Time** | Socket.io `/meetings` handshake authentication, anonymous guest permissions, unhandled Prisma DB crashes, and host disconnect lifecycle. | Socket.io Client (`test/tools/`) | **8/8 PASS** |
| **L4 Desktop UI Automation** | End-to-end user journeys: registration, anonymous guest access, WebRTC video meeting room creation, mic toggle, hand raise, live chat stream, agenda items, and voting polls. | **Java 21 LTS + Selenium 4 + Cucumber BDD 7 + JUnit 5** | **10/10 PASS** |
| **L4 Mobile Web UI** | Responsive layout adaptation, touch-friendly in-call action controls, and mobile navigation drawer in **Pixel 7 (`412x915`)** viewport. | Selenium Chrome Mobile Emulation | **PASS** |
| **Exploratory Testing** | Session-Based Test Management (SBTM) investigating WebRTC media glitches, chat latency/jitter, host disconnect edge cases, and injection security. | 4 Structured Charters (`test-cases/exploratory/`) | **Completed** |

---

## 2. Automation Framework Highlights (`automation/`)

The automation test suite in `automation/` was built to enterprise industry standards:
1. **Full-Stack Java 21 Automation**: Implemented purely in **Java 21 LTS** with **Apache Maven**, covering both UI Automation (**Selenium 4 + Cucumber BDD**) and API Testing (**REST Assured 5.5**).
2. **Deterministic Live Security Reproduction**: Java REST Assured tests (`ApiSecurityDefectTest.java`) programmatically reproduce and assert real security defects (**BUG-001 IDOR**, **BUG-002**, and **BUG-003**) live against backend endpoints.
3. **Strict Zero Fixed Sleep**: 100% synchronized via `WebDriverWait` and `ExpectedConditions` (no `Thread.sleep()`).
4. **WebRTC Fake Media Simulation**: Configured with `--use-fake-device-for-media-stream` and `--use-fake-ui-for-media-stream` to run headless video call tests without physical webcam/microphone hardware.
5. **Cloud Cold-Start Resilience**: Built-in automated warmup probe in `Hooks.java` that pings `GET /health` (`db === 'up'`) before opening the browser, eliminating flakiness from free-tier cloud sleep.

---

## 3. Verified Defects Summary (10 Real Defects)

All defects were discovered, proven with raw network/DB evidence on the live environment, and indexed in [`bug-reports/INDEX.md`](bug-reports/INDEX.md):

| Bug ID | Severity | Title & Root Cause | Spec ID |
|---|---|---|---|
| [**BUG-001**](bug-reports/BUG-001-idor-meeting-metadata-leak.md) | **Critical** | IDOR on `/meetings/:id` leaks private room code and host metadata to unauthorized users | `API-IDOR-01` |
| [**BUG-002**](bug-reports/BUG-002-case-sensitive-room-code-lookup.md) | **Major** | Case-sensitive lookup on `/meetings/code/:code` returns 404 for lowercase room codes | `API-CODE-01` |
| [**BUG-003**](bug-reports/BUG-003-guest-token-users-me-404.md) | **Minor** | Authenticated guest token calling `GET /users/me` returns 404 User Not Found | `AUTH-04` |
| [**BUG-004**](bug-reports/BUG-004-unauthenticated-websocket-handshake-spoofing.md) | **Critical** | WebSocket handshake accepts garbage JWTs, allowing unauthenticated `media:speaking` spoofing | `WS-SEC-01` |
| [**BUG-005**](bug-reports/BUG-005-host-unable-to-kick-or-mute-guest-participants.md) | **Major** | Host moderation controls fail on anonymous guests (cannot kick, mute, or assign co-host) | `WS-GUEST-02` |
| [**BUG-006**](bug-reports/BUG-006-guest-poll-voting-unhandled-database-exception.md) | **Major** | Anonymous guest poll voting throws unhandled Prisma foreign key exception (`P2003`) | `WS-GUEST-01` |
| [**BUG-007**](bug-reports/BUG-007-instant-meeting-deletion-on-solo-host-disconnect.md) | **Critical** | Solo host 2.5s network disconnect instantly destroys meeting without a reconnect grace period | `WS-RECON-03` |
| [**BUG-008**](bug-reports/BUG-008-own-chat-messages-displayed-as-other-sender.md) | **Major** | Own chat messages render on the left with account name instead of on the right as "You" | `MEET-03` |
| [**BUG-009**](bug-reports/BUG-009-lowercase-room-code-causes-joining-screen-hang.md) | **Major** | Lowercase room code input causes indefinite joining screen freeze for registered users | `MEET-06` |
| [**BUG-010**](bug-reports/BUG-010-agenda-progress-bar-stuck-at-45-percent.md) | **Minor** | Agenda "Current Item" progress bar is hard-coded at 45% and never updates dynamically | `UI-AGENDA-03` |

---

## 4. How to Run the Tests

### A. Java 21 UI Automation Suite
```bash
cd automation

# Run Smoke Suite (@smoke):
mvn test "-Dcucumber.filter.tags=@smoke"

# Run Full Regression Suite (10 scenarios):
mvn test

# Run in Headed Mode (to view browser execution):
mvn test -Dheadless=false
```
*Reports generated at `automation/target/cucumber-reports/cucumber.html`.*

### B. API & WebSocket Test Harnesses
```bash
# Set live backend URL (PowerShell):
$env:API_URL="https://beecollab-rwbj.onrender.com"

# 1. Baseline Smoke Tests (88 checks):
node test/tools/smoke-api.mjs
node test/tools/smoke-ws.mjs

# 2. Security, IDOR & Database Defect Suites:
node test/tools/test-api-security.mjs
node test/tools/test-ws-bugs.mjs
```

---

## 5. Repository Structure

```text
├── automation/                  # Java 21 LTS + Selenium + Cucumber BDD Framework
│   ├── pom.xml                  # Maven build configuration
│   └── src/test/java/           # Page Objects, Step Definitions, Drivers, Hooks
├── bug-reports/                 # 10 Detailed Defect Reports + INDEX.md
├── reports/
│   └── final-summary.md         # Executive 1-page QA Sign-Off Report
├── test-cases/
│   ├── test-cases.csv           # Master Test Cases CSV
│   ├── traceability.csv         # 100% Requirements Traceability Matrix
│   └── exploratory/             # 4 Session-Based Exploratory Charters (SBTM)
├── test-plan.md                 # Master QA Test Plan & Architecture Strategy
└── test/                        # System specs (BeeCollab.md) & API/WebSocket test scripts
```

---

## 6. QA Engineering Artifacts & Deliverables
* **Functional Specification**: [`test/BeeCollab.md`](test/BeeCollab.md)
* **Master Test Plan**: [`test-plan.md`](test-plan.md)
* **Traceability Matrix**: [`test-cases/traceability.csv`](test-cases/traceability.csv)
* **Executive Summary**: [`reports/final-summary.md`](reports/final-summary.md)
* **Master Bug Index**: [`bug-reports/INDEX.md`](bug-reports/INDEX.md)
