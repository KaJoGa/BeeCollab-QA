# CH-03 — Host Edge-Case Disconnect, Moderation Failures & Session Lifecycles

| | |
|---|---|
| **Risk Areas** | `R-21` (Instant room destruction on disconnect), `R-06/07` (Host moderation failures on guests) |
| **Spec IDs** | `MOD-01` to `MOD-06`, `RECON-01` to `RECON-03` |
| **Environment** | Live Production (`https://beecollab-rwbj.onrender.com`), Socket.io + REST API |
| **Timebox** | 60 minutes |

---

## Mission
Investigate host authority boundaries, disconnect edge cases, room lifecycle destruction, and host moderation actions against anonymous guest participants.

---

## Why This Matters More Than Scripted Automation
Real-world video collaboration frequently suffers network drops (flaky Wi-Fi, switching networks). If a host's brief disconnect permanently destroys an active meeting, significant user data and context are lost. Similarly, hosts must be able to moderate abusive anonymous guests.

---

## Test Probes & Observations

### 1. Solo Host Network Drop & Room Deletion (R-21)
- **Probe:** Solo host disconnecting socket connection for 2.5 seconds and attempting REST status query.
- **Observation:** **CRITICAL DEFECT CONFIRMED (BUG-007).** Meeting record, agenda, and chat history are wiped permanently from Neon PostgreSQL with zero grace period.

### 2. Host Moderation Over Guest Participants (R-06, R-07, R-07b)
- **Probe:** Host emitting `meeting:kick`, `media:force-mute`, and `meeting:make-cohost` targeting guest socket IDs.
- **Observation:** **MAJOR DEFECT CONFIRMED (BUG-005).** Because guests have no row in the `Participant` database table, `kick` fails with `"Participant not found"`, while `force-mute` and `make-cohost` throw unhandled Prisma 500 exceptions.

### 3. Duplicate Tab Concurrency (R-34)
- **Probe:** Same authenticated user opening meeting room in a secondary browser tab.
- **Observation:** The older socket is disconnected by backend policy. UI on the first tab freezes without an informative reconnect dialog.

---

## Session Notes
* **Execution Date:** 2026-10-10
* **Target Build:** `b6fb446`
* **Defects Triaged:** Logged [`BUG-005`](../../bug-reports/BUG-005-host-unable-to-kick-or-mute-guest-participants.md), [`BUG-007`](../../bug-reports/BUG-007-instant-meeting-deletion-on-solo-host-disconnect.md).
