# BUG-003 — Authenticated Guest Calling GET /users/me Receives 404 User Not Found

| | |
|---|---|
| **Jira Issue** | `Local/Internal` (or `BC-103`) |
| **Spec ID(s)** | `API-AUTH-04` (Related to `R-03`) |
| **Severity** | **Minor / Architectural Inconsistency** (S3 - Boundary Handling) |
| **Found in** | Live Deployment (`https://beecollab-rwbj.onrender.com`), backend commit `b6fb446` |
| **Found during** | Layer 1 API Security & Profile Audit (`tools/test-api-security.mjs`) |

## Summary
When an authenticated guest user with a valid JWT token (`sub: "guest_<uuid>"`, `isGuest: true`) calls the user profile endpoint `GET /users/me`, the server queries the database `User` table. Because guests do not have a persistent row in the `User` table, the query fails and returns `404 Not Found` rather than returning the guest session profile or an explicit architectural error envelope.

## Steps to Reproduce
1. Acquire a guest session token via `POST /auth/guest` with body `{"name": "Guest Charlie"}` -> returns HTTP 201 with `access_token`.
2. Send `GET /users/me` with header `Authorization: Bearer <guest_access_token>`.

## Expected (Per Functional Spec)
The endpoint should inspect the JWT payload; if `isGuest: true`, it should return the ephemeral guest profile (e.g. `{ id: "guest_<uuid>", name: "Guest Charlie", isGuest: true }`) or a descriptive business response, rather than an unhandled 404 entity lookup error.

## Actual
```json
HTTP/1.1 404 Not Found
Content-Type: application/json; charset=utf-8

{
  "success": false,
  "error": "User not found",
  "statusCode": 404,
  "timestamp": "2026-10-10T10:42:30.232Z",
  "path": "/users/me"
}
```

## Root Cause & Impact
The `UsersService.getProfile` assumes all authenticated JWTs map directly to a record in `User` via `prisma.user.findUnique({ where: { id: req.user.sub } })`. When the lookup returns null, it throws `NotFoundException('User not found')`. Any frontend component attempting to refresh or verify profile state via `/users/me` will fail when the active user is a guest.
