# BUG-001 — Insecure Direct Object Reference (IDOR) Exposes Meeting Metadata, Chat, and Private Room Code

| | |
|---|---|
| **Jira Issue** | `Local/Internal` (or `BC-101`) |
| **Spec ID(s)** | `API-IDOR-01`, `API-IDOR-02`, `API-IDOR-03`, `API-IDOR-04` (Related to `R-04`) |
| **Severity** | **Critical** (S1 - Information Disclosure / Authorization Bypass) |
| **Found in** | Live Deployment (`https://beecollab-rwbj.onrender.com`), backend commit `b6fb446` |
| **Found during** | Layer 1 & 2 API Security Audit (`tools/test-api-security.mjs`) |

## Summary
The REST API endpoints `GET /meetings/:id`, `GET /meetings/:id/chat`, and `GET /meetings/:id/participants` do not verify whether the requesting user is a member or invited attendee of the meeting. Any authenticated user (including ephemeral guest accounts) who knows or obtains a meeting UUID can read confidential meeting metadata, full chat history, active participant identities, and the private 8-character uppercase room code.

## Steps to Reproduce
1. Authenticate as User A (Host) and create a private meeting via `POST /meetings`. Record the meeting UUID (`idA`) and room code (`roomCodeA`).
2. Authenticate as User B (an unrelated registered user who has neither joined nor received an invitation to Meeting A).
3. Send `GET /meetings/:idA` with User B's Bearer token.
4. Send `GET /meetings/:idA/chat` with User B's Bearer token.
5. Send `GET /meetings/:idA/participants` with User B's Bearer token.
6. Authenticate as Guest C via `POST /auth/guest` and send `GET /meetings/:idA` with Guest C's token.

## Expected (Per Functional Spec & Security Standards)
Requests to inspect private meeting details, chat logs, or participants should enforce room authorization and return `403 Forbidden` or `404 Not Found` for non-members and uninvited users. The private `roomCode` should never be disclosed to non-hosts.

## Actual
- `GET /meetings/:idA` returns `HTTP 200 OK` exposing `title`, `hostId`, and `roomCode`.
- `GET /meetings/:idA/chat` returns `HTTP 200 OK` exposing the array of stored chat messages.
- `GET /meetings/:idA/participants` returns `HTTP 200 OK` exposing active participant details.
- Guest C querying `GET /meetings/:idA` receives `HTTP 200 OK` and successfully extracts the private 8-hex `roomCode`.

## Evidence
Raw response from `tools/test-api-security.mjs` against live deployment:
```json
HTTP/1.1 200 OK
Content-Type: application/json; charset=utf-8

{
  "success": true,
  "data": {
    "id": "c136f7fd-eb61-4d69-a224-dd9954af1efd",
    "title": "Alice Confidential Strategy",
    "roomCode": "8BADDB42",
    "hostId": "79910ddf-87cb-4ea1-a9dd-1c1a1e826eb8",
    "status": "SCHEDULED",
    "maxParticipants": 5,
    "duration": 30
  },
  "message": "Success",
  "timestamp": "2026-10-10T10:42:28.100Z",
  "path": "/meetings/c136f7fd-eb61-4d69-a224-dd9954af1efd"
}
```

## Root Cause & Impact
In `meetings.controller.ts` / `meetings.service.ts`, the query retrieves the meeting directly via Prisma `findUnique({ where: { id } })` without scoping the query to `req.user.id` or checking the `Participant` table. Any user can scrape meeting data or bypass joining restrictions by reading the room code directly from the API.
