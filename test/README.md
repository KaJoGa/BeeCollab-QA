# BeeCollab — API & WebSocket Test Harness

This directory contains the system specifications and automated testing harnesses for **Layer 1 (REST API Contracts & Security)** and **Layer 3 (WebSocket Real-Time Gateway)** of BeeCollab.

For the primary end-to-end UI automation suite (Layer 4), refer to the Java 21 LTS Maven project in `automation/`.

---

## 1. System & Testing Specification
* [**BeeCollab.md**](BeeCollab.md): Complete system architecture, WebRTC full-mesh mechanics, identity roles, and full functional testing specifications (`AUTH`, `HOME`, `ROOM`, `MED`, `COL`, `HOST`, `SEC`).

---

## 2. Tools & Scripts Overview (`tools/`)
The `tools/` folder provides standalone Node.js test harnesses for API contracts, horizontal privilege escalation (IDOR), token security attacks, and real-time Socket.io concurrency:

| Script | Purpose & Coverage |
|---|---|
| `smoke-api.mjs` | 47 automated REST checks covering health, authentication, room lifecycle, boundary limits, and error envelopes. |
| `smoke-ws.mjs` | 41 automated WebSocket checks covering namespace handshakes, presence, media events, reactions, and capacity limits. |
| `test-api-security.mjs` | Security validation proving IDOR vulnerabilities (`BUG-001`), case sensitivity (`BUG-002`), guest 404 (`BUG-003`), token tampering (`alg: none`), and input fuzzing (10k strings, XSS, SQLi). |
| `test-ws-bugs.mjs` | Real-time defect suite proving handshake token bypass (`BUG-004`), guest moderation failure (`BUG-005`), guest poll vote DB crash (`BUG-006`), and solo host disconnect room deletion (`BUG-007`). |
| `wait-for-deploy.mjs` | Automated polling probe monitoring backend `/health` until database connectivity is verified (`db === 'up'`). |
| `config.mjs` | Shared test configuration, disposable account generators, API helper, and test reporter. |

---

## 3. How to Execute Test Tools
```bash
# Set live backend URL (PowerShell):
$env:API_URL="https://beecollab-rwbj.onrender.com"

# Install dependencies:
npm install

# Run Baseline Smoke Tests:
node tools/smoke-api.mjs
node tools/smoke-ws.mjs

# Run Security & Defect Reproduction Suites:
node tools/test-api-security.mjs
node tools/test-ws-bugs.mjs
```
