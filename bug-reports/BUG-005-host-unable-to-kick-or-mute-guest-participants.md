# BUG-005 — Host Moderation Controls Completely Fail on Guest Attendees (Cannot Kick, Mute, or Assign Co-Host)

| | |
|---|---|
| **Jira Issue** | `Local/Internal` (or `BC-105`) |
| **Spec ID(s)** | `WS-GUEST-02`, `WS-GUEST-03`, `WS-GUEST-04` (Related to `R-06`, `R-07`, `R-07b`) |
| **Severity** | **Major** (S2 - Core Host Moderation Defective) |
| **Found in** | Live Deployment (`wss://beecollab-rwbj.onrender.com/meetings`), backend commit `b6fb446` |
| **Found during** | Layer 3 WebSocket Real-Time Defect Suite (`tools/test-ws-bugs.mjs`) |

## Summary
Meeting hosts are unable to moderate or remove guest participants from active meetings. Emitting `meeting:kick` against a guest returns `"Participant not found"` and leaves the guest in the meeting. Emitting `media:force-mute` or `meeting:make-cohost` crashes the server-side event handler with an unhandled database exception (`Internal server error`, Prisma `P2025`).

## Steps to Reproduce
1. Host Alice creates a meeting and joins via WebSocket.
2. Guest Bob joins the same meeting using a valid guest token.
3. Host Alice clicks "Kick" on Guest Bob (emits `meeting:kick` with Guest Bob's `socketId`).
4. Host Alice clicks "Force Mute" on Guest Bob (emits `media:force-mute` with Guest Bob's `socketId`).
5. Host Alice attempts to promote Guest Bob to Co-Host (emits `meeting:make-cohost`).

## Expected (Per Functional Spec)
1. Kicking a guest should disconnect their socket, emit `meeting:kicked` to the guest, and broadcast `participant:left` to the room.
2. Force-muting a guest should update their audio state to disabled and broadcast `media:updated`.
3. Promoting a guest to co-host should update their room role or provide a clear operational rejection.

## Actual
1. **Kick Failure:** Host receives error event `{"message": "Participant not found"}`. Guest receives no event, is not disconnected, and remains active in the meeting.
2. **Force-Mute Crash:** Host receives unhandled server exception:
   `{"status": "error", "message": "Internal server error", "cause": {"pattern": "media:force-mute", "data": {...}}}`
3. **Make Co-Host Crash:** Host receives unhandled server exception:
   `{"status": "error", "message": "Internal server error", "cause": {"pattern": "meeting:make-cohost", "data": {...}}}`

## Evidence
Raw event transcript from `tools/test-ws-bugs.mjs`:
```
[R-06 Check] Host received error on kick guest: {"message":"Participant not found"}
[R-06 Check] Guest received meeting:kicked event: false
PASS  Host kick guest fails with "Participant not found" (R-06 verified)
[R-07 Check] Host received exception/error on force-mute guest: {"status":"error","message":"Internal server error","cause":{"pattern":"media:force-mute","data":{"meetingId":"b822bffc-e581-4bb3-8a00-c56372aedf0c","targetSocketId":"fSN7FCzHs5VG713-AAAB"}}}
PASS  Host force-mute guest triggers database exception (R-07 verified)
```

## Root Cause & Impact
The database schema defines `Participant` with a foreign key requirement referencing the `User` table (`userId -> User.id`). Because guest accounts are ephemeral and have no row in `User`, no `Participant` record is ever inserted for guests. Downstream moderation queries execute `prisma.participant.findUnique` or `prisma.participant.update`, which throws `P2025: Record to update not found` or fails the participant lookup completely.
As a result, malicious or disruptive guest participants cannot be ejected or muted by the meeting host.
