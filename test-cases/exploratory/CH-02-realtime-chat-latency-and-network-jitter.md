# CH-02 — Real-Time Chat Latency, Message Ordering & WebSocket Resilience

| | |
|---|---|
| **Risk Areas** | `R-24` (Own chat message layout bug), `R-07c` (Guest ephemeral chat), `R-25` (Timestamps) |
| **Spec IDs** | `CHAT-01` to `CHAT-05`, `MEET-03` |
| **Environment** | Live Production (`https://beecollab.vercel.app`), Socket.io `/meetings` |
| **Timebox** | 45 minutes |

---

## Mission
Probe Socket.io `/meetings` namespace chat message delivery under high frequency typing, special character injections, guest ephemeral lifecycles, and layout attribution logic.

---

## Why This Matters More Than Scripted Automation
Real-time chat in a video meeting requires sub-second delivery, persistence for registered participants, and correct alignment (incoming vs outgoing). Exploratory probing investigates edge cases like zero-width spaces, rapid Enter strokes, and tab-switching latency.

---

## Test Probes & Observations

### 1. Own Message Sender Attribution (R-24)
- **Probe:** Local user sending messages and inspecting visual alignment and metadata.
- **Observation:** **DEFECT CONFIRMED (BUG-008).** Sent message appears on the left with local account name rather than on the right as "You". Domestic `data-me` attribute is missing.

### 2. Guest Ephemeral Chat Persistence
- **Probe:** Guest sending messages, followed by a new participant joining late.
- **Observation:** Guest messages are broadcast in real-time to active participants. However, `GET /meetings/:id/chat` does not persist guest messages due to foreign key constraints on the `User` table. Late joiners only receive registered user history.

### 3. Rapid Keystroke Ingestion & Empty Input
- **Probe:** Sending whitespace-only strings and rapid multi-line text.
- **Observation:** Send button (`chat-send`) correctly disables when input is empty or whitespace-only. `chat-input` clears immediately upon message transmission.

---

## Session Notes
* **Execution Date:** 2026-10-10
* **Target Build:** `b6fb446`
* **Defects Triaged:** Logged [`BUG-008`](../../bug-reports/BUG-008-own-chat-messages-displayed-as-other-sender.md).
