# BUG-006 — Guest Poll Voting Throws Unhandled Prisma Foreign Key Constraint Exception

| | |
|---|---|
| **Jira Issue** | `Local/Internal` (or `BC-106`) |
| **Spec ID(s)** | `WS-GUEST-01`, `POLL-15` (Related to `R-05`) |
| **Severity** | **Major** (S2 - Core Collaboration Feature Crashes) |
| **Found in** | Live Deployment (`wss://beecollab-rwbj.onrender.com/meetings`), backend commit `b6fb446` |
| **Found during** | Layer 3 WebSocket Real-Time Defect Suite (`tools/test-ws-bugs.mjs`) |

## Summary
The interactive polling feature enables hosts to launch real-time polls during a meeting. The frontend user interface displays poll cards and clickable voting options to all connected participants, including guests. However, when a guest submits a vote (`poll:vote`), the backend attempts to persist the vote into `PollResponse` where `userId` is a mandatory foreign key referencing the `User` table. Because guests have no record in `User`, Prisma throws an unhandled database Foreign Key Constraint Violation (`P2003`), returning an internal server error exception to the client while failing to count the vote.

## Steps to Reproduce
1. Host Alice creates and launches a poll in an active meeting room with two options ("Tacos" and "Burgers").
2. Guest Bob connects to the meeting and receives the `poll:created` event.
3. Guest Bob clicks on Option 1 to cast a vote (client emits `poll:vote` with `{ meetingId, pollId, optionId }`).

## Expected (Per Functional Spec)
Either:
- Guest votes should be supported (e.g. by allowing null/ephemeral `userId` in `PollResponse` or storing guest vote counts in memory/session), OR
- The UI should disable the vote button for guests with a clear explanatory tooltip, and the backend should reject guest votes gracefully with an operational error rather than an unhandled 500 exception.

## Actual
Guest Bob receives an unhandled NestJS exception event:
```json
{
  "status": "error",
  "message": "Internal server error",
  "cause": {
    "pattern": "poll:vote",
    "data": {
      "meetingId": "b822bffc-e581-4bb3-8a00-c56372aedf0c",
      "pollId": "ad4f567b-4c82-4d99-a3fd-5c8a2be1bcc8",
      "optionId": "48dbb416-aed2-40bc-94af-e99e8e5fa5a8"
    }
  }
}
```
No `poll:updated` event is broadcast to the meeting room, and the vote is discarded.

## Evidence
Log output from `tools/test-ws-bugs.mjs`:
```
[Poll Setup] Poll created: id=ad4f567b-4c82-4d99-a3fd-5c8a2be1bcc8
[R-05 Check] Guest received exception/error on poll:vote: {"status":"error","message":"Internal server error","cause":{"pattern":"poll:vote", ...}}
[R-05 Check] Did poll update with guest vote: false
PASS  Guest voting triggers exception due to missing User FK (R-05 verified)
```

## Root Cause & Impact
In `schema.prisma`:
```prisma
model PollResponse {
  id       String     @id @default(uuid())
  pollId   String
  optionId String
  userId   String
  user     User       @relation(fields: [userId], references: [id])
}
```
`userId` is non-nullable and strictly tied to `User`. The gateway handler passes `req.user.sub` directly into `prisma.pollResponse.create()`. For guests, `req.user.sub` is a synthetic string (`guest_<uuid>`), causing PostgreSQL to reject the insert.
