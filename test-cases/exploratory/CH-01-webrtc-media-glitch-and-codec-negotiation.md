# CH-01 — WebRTC Media Streams, Codec Negotiation & Mesh Scalability

| | |
|---|---|
| **Risk Areas** | `R-35` (Full-mesh O(N²) scaling), `D1-D5` (Media controls, fake tracks, Bitrate caps) |
| **Spec IDs** | `MEDIA-01` to `MEDIA-05`, `MEET-02` |
| **Environment** | Live Production (`https://beecollab.vercel.app`), Chrome Headless + Emulated Devices |
| **Timebox** | 60 minutes |

---

## Mission
Explore WebRTC media stream behavior under multi-peer scenarios, camera/microphone renegotiation, device permissions toggling, and network quality indicators to evaluate mesh stability and user feedback.

---

## Why This Matters More Than Scripted Automation
WebRTC connections involve asynchronous STUN/TURN ICE candidate gathering, SDP offer/answer exchanges, and browser hardware constraints. While scripted tests toggle button attributes, exploratory probing reveals actual track state transitions, renegotiation deadlocks, and bandwidth degradation.

---

## Test Probes & Observations

### 1. Camera & Microphone Track Renegotiation
- **Probe:** Rapidly toggling microphone and camera on and off in succession (<200ms intervals).
- **Observation:** Audio track transitions cleanly (`data-enabled="false" / "true"`). Avatar fallback renders when video is disabled. No media pipeline freeze detected.

### 2. Mesh Bitrate & Fake Track Ingestion
- **Probe:** Inspecting `RTCRtpSender.getParameters()` with virtual media pattern generators.
- **Observation:** Chrome successfully encodes test pattern video stream (`videoWidth > 0`). ICE servers resolve via `GET /webrtc/ice-servers`.

### 3. Data Saver Mode
- **Probe:** Activating `ctrl-data-saver` during active media transmission.
- **Observation:** Inbound remote video track subscription suspends; UI replaces video tiles with user initial avatars without terminating peer connection.

---

## Session Notes
* **Execution Date:** 2026-10-10
* **Target Build:** `b6fb446`
* **Defects Triaged:** None blocking in 2-peer call; verified quadratic bandwidth penalty documented under architecture risk `R-35`.
