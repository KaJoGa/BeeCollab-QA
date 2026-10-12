# CH-04 — Adversarial Security, IDOR Leaks & Injection Attacks

| | |
|---|---|
| **Risk Areas** | `R-04` (IDOR on meetings/chat/participants), `R-09` (Handshake bypass), `R-16` (Case-sensitivity) |
| **Spec IDs** | `SEC-01` to `SEC-05`, `API-IDOR-01`, `API-CODE-01` |
| **Environment** | Live Production (`https://beecollab-rwbj.onrender.com`), Postman / Node Test Harness |
| **Timebox** | 60 minutes |

---

## Mission
Probe authentication boundaries, horizontal privilege escalation (IDOR), JWT signature spoofing, and input sanitization across REST endpoints and Socket.io gateways.

---

## Why This Matters More Than Scripted Automation
Security boundaries must protect sensitive room codes, participant rosters, and private meetings against unauthorized scraping and tampering. Automated functional tests verify only legitimate flows; adversarial exploratory probing tests unauthorized vectors.

---

## Test Probes & Observations

### 1. Insecure Direct Object Reference (R-04)
- **Probe:** User B querying `GET /meetings/:id`, `/chat`, and `/participants` for Meeting A created by User A.
- **Observation:** **CRITICAL DEFECT CONFIRMED (BUG-001).** Unrelated members and guests can read complete meeting details, participant roster, and private 8-hex room codes knowing only the UUID.

### 2. WebSocket Handshake Bypass & Speaking Spoofing (R-09)
- **Probe:** Establishing Socket.io handshake using invalid token `Bearer invalid.fake.token` and emitting `media:speaking`.
- **Observation:** **CRITICAL DEFECT CONFIRMED (BUG-004).** Handshake accepts garbage tokens without rejection. Unauthenticated socket can spoof speaking events into active rooms.

### 3. Room Code Case Sensitivity & Lowercase Lookup (R-16, R-17)
- **Probe:** Querying `GET /meetings/code/:code` with lowercase hexadecimal characters.
- **Observation:** **MAJOR DEFECT CONFIRMED (BUG-002, BUG-009).** Uppercase succeeds (`200 OK`), lowercase returns `404 Not Found`, causing joining screen UI freeze.

### 4. Input Boundary & Fuzzing Payloads
- **Probe:** Injecting 10,000-character titles, XSS payloads (`<script>alert(1)</script>`), and SQLi strings (`' OR 1=1; --`).
- **Observation:** Extreme length is truncated/stored safely (201). XSS strings are escaped by React JSX. SQLi attempts are blocked safely (403), with no 500 database crashes.

---

## Session Notes
* **Execution Date:** 2026-10-10
* **Target Build:** `b6fb446`
* **Defects Triaged:** Logged [`BUG-001`](../../bug-reports/BUG-001-idor-meeting-metadata-leak.md), [`BUG-002`](../../bug-reports/BUG-002-case-sensitive-room-code-lookup.md), [`BUG-004`](../../bug-reports/BUG-004-unauthenticated-websocket-handshake-spoofing.md).
