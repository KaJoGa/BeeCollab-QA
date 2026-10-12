# BUG-007 — Brief Network Disconnect by Solo Host Permanently Destroys Meeting and Stored Chat History

| | |
|---|---|
| **Jira Issue** | `Local/Internal` (or `BC-107`) |
| **Spec ID(s)`** | `WS-RECON-03`, `ROOM-31` (Related to `R-21`) |
| **Severity** | **Critical** (S1 - Premature Data Destruction / Zero Reconnection Grace Period) |
| **Found in** | Live Deployment (`https://beecollab-rwbj.onrender.com`), backend commit `b6fb446` |
| **Found during** | Layer 3 WebSocket Lifecycle Audit (`tools/test-ws-bugs.mjs`) |

## Summary
The backend architecture implements an auto-cleanup listener (`meeting-events.listener.ts`) that immediately deletes a meeting from the PostgreSQL database the moment its last participant disconnects. If a host enters a scheduled meeting room before other participants have joined, any transient network disruption (such as a 2-second Wi-Fi glitch, browser tab refresh, or network interface switch) triggers the empty-room listener. The meeting, its configuration, and any pre-existing chat history are permanently deleted from the database. Subsequent reconnection attempts fail with `404 Not Found`.

## Steps to Reproduce
1. Authenticate as Host Alice and create a scheduled meeting with a 30-minute duration.
2. Host Alice connects via WebSocket and joins the meeting room (meeting status changes to `LIVE`).
3. Verify via REST `GET /meetings/:id` that the meeting is active (`HTTP 200 OK`).
4. Disconnect Host Alice's WebSocket connection (simulating a momentary network drop or page refresh).
5. Wait 2.5 seconds.
6. Attempt to query `GET /meetings/:id` or reconnect to the room.

## Expected (Per Functional Spec & Best Practices)
A meeting with remaining scheduled duration should provide a configurable reconnection grace period (e.g. 60–120 seconds) before executing hard teardown. Momentary network fluctuations by a solo host should not result in immediate and irreversible room destruction.

## Actual
Within 2 seconds of the socket disconnect, the backend hard-deletes the meeting record and cascades deletion across all associated child records (participants, agenda items, chat messages).
- `GET /meetings/:id` returns `HTTP 404 Not Found`.
- Reconnection attempts fail, and the host is permanently locked out of their own scheduled meeting.

## Evidence
Log output from `tools/test-ws-bugs.mjs`:
```
Solo host joined meeting b7cc88d4-2f05-4b97-b39f-9a38c2c0289b. Verifying live state via REST...
PASS  Meeting is LIVE while host connected -> 200
Disconnecting solo host socket...
[R-21 Check] GET /meetings/:id after 2.5s solo disconnect -> HTTP 404
PASS  Meeting is immediately destroyed on solo disconnect (R-21 verified)
```

## Root Cause & Impact
In `meeting-events.listener.ts`:
```typescript
@OnEvent('meeting.participant.left')
async handleParticipantLeft(event: ParticipantLeftEvent) {
  const remaining = await this.participantRepo.countActive(event.meetingId);
  if (remaining === 0) {
    await this.meetingRepo.deleteMeeting(event.meetingId); // Immediate deletion!
  }
}
```
No grace timer or debounce delay is implemented. This creates catastrophic instability for hosts on mobile connections or unstable internet who refresh the page or momentarily lose signal.
