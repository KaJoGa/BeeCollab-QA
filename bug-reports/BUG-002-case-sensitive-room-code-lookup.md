# BUG-002 — Case-Sensitive Room Code Lookup Causes 404 and Frontend Joining Screen Freeze

| | |
|---|---|
| **Jira Issue** | `Local/Internal` (or `BC-102`) |
| **Spec ID(s)** | `API-CODE-01`, `HOME-15` (Related to `R-16`, `R-17`) |
| **Severity** | **Major** (S2 - Core Join Flow Degraded) |
| **Found in** | Live Deployment (`https://beecollab-rwbj.onrender.com`), backend commit `b6fb446` |
| **Found during** | Layer 1 API Contract & Security Audit (`tools/test-api-security.mjs`) |

## Summary
The endpoint `GET /meetings/code/:roomCode` performs an exact case-sensitive lookup in PostgreSQL. While generated room codes are uppercase 8-character hex strings (e.g. `8BADDB42`), querying with lowercase characters (e.g. `8baddb42`) fails with `404 Not Found`. Because the frontend home page only automatically capitalizes room codes for guests, registered users who type lowercase codes encounter a silent 404 that causes the joining screen to hang indefinitely.

## Steps to Reproduce
1. Create a meeting and obtain its 8-character uppercase room code (e.g. `8BADDB42`).
2. Send `GET /meetings/code/8BADDB42` with an authorization token -> returns `200 OK`.
3. Send `GET /meetings/code/8baddb42` (converted to lowercase) with the same token -> returns `404 Not Found`.

## Expected (Per Functional Spec)
Hexadecimal room codes are case-insensitive identifiers. The server should either convert the input parameter to uppercase (`roomCode.toUpperCase()`) or perform a case-insensitive query (`mode: 'insensitive'` in Prisma).

## Actual
```json
HTTP/1.1 404 Not Found
Content-Type: application/json; charset=utf-8

{
  "success": false,
  "error": "Meeting not found with code: 8baddb42",
  "statusCode": 404,
  "timestamp": "2026-10-10T10:42:29.800Z",
  "path": "/meetings/code/8baddb42"
}
```

## Root Cause & Impact
Backend query matches `where: { roomCode }` strictly without normalization. In the frontend, registered users navigating via home page input (`join-code-input`) are routed to `/meeting/8baddb42`, where the client attempts to resolve the meeting metadata, receives 404, and hangs forever on `joining-screen` with no user feedback.
