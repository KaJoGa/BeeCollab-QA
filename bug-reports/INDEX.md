# Master Bug Index

A centralized registry of all defects identified, triaged, and verified across all testing layers for **BeeCollab**.

| ID | Title | Spec ID | Severity | Issue Key | Status |
|---|---|---|---|---|---|
| [BUG-001](BUG-001-idor-meeting-metadata-leak.md) | Insecure Direct Object Reference (IDOR) on `/meetings/:id`, `/chat`, and `/participants` leaks metadata, history, and room code | `API-IDOR-01` (`R-04`) | **Critical** | BC-101 | **Open** (Verified on Live) |
| [BUG-002](BUG-002-case-sensitive-room-code-lookup.md) | Case-sensitive room code lookup on `/meetings/code/:code` causes 404 and frontend freeze | `API-CODE-01` (`R-16`, `R-17`) | **Major** | BC-102 | **Open** (Verified on Live) |
| [BUG-003](BUG-003-guest-token-users-me-404.md) | Authenticated guest calling `GET /users/me` receives 404 User Not Found | `API-AUTH-04` (`R-03`) | **Minor** | BC-103 | **Open** (Verified on Live) |
| [BUG-004](BUG-004-unauthenticated-websocket-handshake-spoofing.md) | WebSocket handshake accepts invalid JWT tokens allowing unauthenticated speaking event spoofing | `WS-SEC-01` (`R-09`) | **Critical** | BC-104 | **Open** (Verified on Live) |
| [BUG-005](BUG-005-host-unable-to-kick-or-mute-guest-participants.md) | Host moderation controls fail on guests (cannot kick, mute, or make co-host due to missing DB row) | `WS-GUEST-02` (`R-06`, `R-07`) | **Major** | BC-105 | **Open** (Verified on Live) |
| [BUG-006](BUG-006-guest-poll-voting-unhandled-database-exception.md) | Guest poll voting throws unhandled Prisma foreign key constraint exception | `WS-GUEST-01` (`R-05`) | **Major** | BC-106 | **Open** (Verified on Live) |
| [BUG-007](BUG-007-instant-meeting-deletion-on-solo-host-disconnect.md) | Brief network disconnect by solo host permanently destroys meeting and chat history (no grace period) | `WS-RECON-03` (`R-21`) | **Critical** | BC-107 | **Open** (Verified on Live) |
| [BUG-008](BUG-008-own-chat-messages-displayed-as-other-sender.md) | Own chat messages displayed as external participant with left-aligned layout | `UI-CHAT-01` (`R-24`) | **Major** | BC-108 | **Open** (Verified on Live) |
| [BUG-009](BUG-009-lowercase-room-code-causes-joining-screen-hang.md) | Lowercase room code input causes indefinite joining screen hang for registered users | `UI-JOIN-02` (`R-17`, `R-18`) | **Major** | BC-109 | **Open** (Verified on Live) |
| [BUG-010](BUG-010-agenda-progress-bar-stuck-at-45-percent.md) | Agenda "Current Item" progress bar is hard-coded at 45% and never advances | `UI-AGENDA-03` (`R-26`) | **Minor** | BC-110 | **Open** (Verified on Live) |

---

## Status Classification Legend

- **Open:** Active defect verified on target environment, pending developer remediation.
- **Fixed:** Defect resolved by development team and verified by QA on a clean test run.
- **Closed (spec changed):** Behavior confirmed as intended product evolution; functional specification updated.
- **Closed (deferred):** Valid defect deprioritized by product management for a future release cycle.
- **Closed (accepted):** Minor edge case accepted as a known operational trade-off.
- **Withdrawn:** Discarded after retesting confirmed a flawed test harness precondition.
