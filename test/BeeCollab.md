# BeeCollab — System Documentation & Testing Specifications

This document provides a comprehensive overview of **BeeCollab** along with **Feature Specifications for Testing**, structured for Quality Assurance (QA) test design, automation, and execution.

---

## Technical Overview

**BeeCollab** is an interactive, real-time web video collaboration and meeting platform (*Google Meet-style*) built on a **Peer-to-Peer (P2P) Full Mesh WebRTC** architecture.

Key Architectural Characteristics:
* **No Central Media Server (SFU/MCU)**: Audio, video, and screen-sharing media streams flow directly between participant browsers via WebRTC using STUN or Cloudflare TURN relays.
* **Backend as Signaling & State Manager**: The backend handles user authentication, meeting scheduling/room state, WebRTC signaling coordination (*offer, answer, ICE candidates*), and collaborative real-time feature synchronization (*in-call chat*, hand-raise queue, agenda items, voting polls, and reactions) via REST APIs and WebSocket (Socket.io `/meetings` namespace).

### Technical Stack
* **Frontend**: Next.js 16 (App Router), React 19, TypeScript, Tailwind CSS / CSS Modules (Hosted on Vercel).
* **Backend API**: NestJS 11, Socket.io, Passport JWT, Prisma 7 (`@prisma/adapter-pg`), `@nestjs/schedule` (Hosted on Render free tier).
* **Database**: PostgreSQL (Neon Serverless PostgreSQL).
* **NAT Traversal / Relay**: Public STUN + Cloudflare Realtime TURN (ephemeral 24-hour dynamic credentials).

### Identity Model & Roles
1. **Registered User**: Permanent account persisted in database (`User` table). Features unique email, hashed password, and user profile. JWT access token payload: `{ sub: <uuid>, email }`.
2. **Guest User**: Anonymous participant without a database record. Enters a display name only. Issues an instant guest JWT token with payload: `{ sub: "guest_<uuid>", name, isGuest: true }`.
3. **In-Meeting Roles**:
   * `HOST`: Room creator. Holds full moderation authority (end meeting for all, promote co-hosts, kick, force-mute, create agenda items, launch polls).
   * `CO_HOST`: Participant promoted by the Host. Holds moderation capabilities (cannot kick or mute the Host).
   * `PARTICIPANT`: Regular meeting participant or anonymous guest.

### Meeting Lifecycle
1. **Creation (`SCHEDULED`)**: Created via `POST /meetings`. Generates an 8-character uppercase hexadecimal room code (e.g. `A1B2C3D4`).
2. **In-Progress (`LIVE`)**: Transitioned to `LIVE` as soon as the first participant connects via WebSocket (`meeting:join`).
3. **Termination & Cleanup (`HARD DELETE`)**: The meeting record and all cascaded child records (participants, agenda items, polls, chat messages) are hard-deleted from PostgreSQL when:
   * The host explicitly ends the meeting (`meeting:end` / `DELETE /meetings/:id`),
   * The last remaining participant disconnects (*last participant disconnect*), OR
   * The scheduled duration expires (background scheduler sweeps every 30 seconds).
   * *Note*: The platform does not retain historical records of ended meetings; subsequent lookups return HTTP 404 Not Found.

---

## 1. Authentication & User Profile (`AUTH`)

| ID | Action / Condition | Expected Result |
|---|---|---|
| AUTH-01 | User registers (`POST /auth/register` or Sign Up UI) with valid email, display name (>=2 chars), and password (>=6 chars). | HTTP 201 returns `access_token`; UI automatically signs in and redirects user to home dashboard (`/`) displaying user name. |
| AUTH-02 | Registration with an email that is already registered. | HTTP 409 Conflict with message `"Email already in use"`; UI shows inline error without wiping the form. |
| AUTH-03 | Registration with malformed email, name < 2 chars, or password < 6 chars. | HTTP 400 Bad Request with validation details; browser form validation blocks submission before dispatch. |
| AUTH-04 | Registration payload contains unknown/unwhitelisted fields (e.g. `role: "admin"`). | HTTP 400 Bad Request (`whitelist & forbidNonWhitelisted` rejects unknown properties). |
| AUTH-05 | User login (`POST /auth/login` or Sign In UI) with valid email and password. | HTTP 201 returns `access_token`; token stored in `localStorage ('token')` and redirects to dashboard. |
| AUTH-06 | Login with invalid password or unregistered email. | HTTP 401 Unauthorized with generic message `"Invalid credentials"` (prevents user enumeration); UI remains on `/login` and displays error banner. |
| AUTH-07 | Join as guest (`POST /auth/guest` or Guest form UI) with name >= 2 chars. | HTTP 201 returns guest JWT with `isGuest: true`; UI stores `guestName` in `localStorage`, displays `"Guest: <Name>"`, and logout button shows `"Exit Guest"`. |
| AUTH-08 | Join as guest with empty name or name < 2 chars. | HTTP 400 Bad Request; UI "Continue as Guest" button remains disabled. |
| AUTH-09 | Entering password in registration and login forms. | Dynamic password hint progresses (`Minimum 6 characters (x/6)`), and eye toggle switches input between `password` and `text`. |
| AUTH-10 | Accessing `/login` while possessing a valid session token in `localStorage`. | User is automatically redirected to home dashboard (`/`). |
| AUTH-11 | Clicking Logout button on navbar or home page. | Confirmation dialog opens. Clicking "Cancel" keeps session active; clicking "Confirm" clears token and returns to unauthenticated state. |
| AUTH-12 | Accessing application with malformed, forged, or expired JWT token. | API returns 401 Unauthorized; frontend clears invalid token from `localStorage` and resets UI to sign-in state. |
| AUTH-13 | Registered user calls `GET /users/me` with valid Bearer token. | HTTP 200 returns user profile `{ id, email, name, avatarUrl, createdAt }` without exposing `passwordHash`. |
| AUTH-14 | Guest user calls `GET /users/me`. | HTTP 404 Not Found `"User not found"` because guest accounts do not have database rows in `User` table. |

---

## 2. Home & Meeting Setup (`HOME`)

| ID | Action / Condition | Expected Result |
|---|---|---|
| HOME-01 | Accessing home (`/`), `/login`, or meeting link during backend cold start. | Wake-on-open component detects no response, displays banner `"Waking up the server, this can take up to a minute…"`, polls `GET /health` every 3s, and auto-dismisses when `db: "up"`. |
| HOME-02 | Server status indicator on home page header. | Displays `"System Online"` badge when `/health` responds with `db: "up"`, or `"System Offline"` if request fails. |
| HOME-03 | Authenticated user opens "New Meeting" modal, fills title (>=3 chars), duration (1-1440 min), and capacity (2-10 in UI). | Form validates, sends `POST /meetings`, receives HTTP 201 with meeting object and 8-character hex room code, and redirects browser to `/meeting/<UUID>`. |
| HOME-04 | Creating meeting with capacity outside allowable bounds (< 2 or > 10 in UI, or > 500 via API). | UI displays validation error `"between 2 and 10"` and blocks submit; API returns HTTP 400 Bad Request. |
| HOME-05 | Cancelling meeting creation via "Cancel" button in modal. | Modal closes, inputs reset, and home dashboard remains unchanged. |
| HOME-06 | User enters 8-character room code in "Enter Room Code" input and clicks "Join" or presses Enter. | Client calls `GET /meetings/code/<ROOM_CODE>`, retrieves UUID, and navigates to `/meeting/<UUID>`. Join button disabled if input is empty. |
| HOME-07 | Unauthenticated guest uses "Join as Guest" panel with display name and room code. | Room code is automatically converted to uppercase; guest token issued and user navigates directly into meeting room. |
| HOME-08 | Guest user attempts to click "New Meeting" button on home page. | Button is disabled or inactive for guest users. |
| HOME-09 | Directly accessing `/meeting/<UUID>` without an active authentication token. | Automatically redirected to login with redirect query: `/login?redirect=/meeting/<UUID>`. |
| HOME-10 | Accessing meeting URL with non-existent or deleted UUID. | After API lookup returns 404, user is redirected back to home page (`/`). |

---

## 3. Room Presence & UI (`ROOM`)

| ID | Action / Condition | Expected Result |
|---|---|---|
| ROOM-01 | Host opens meeting page `/meeting/<UUID>`. | Joining screen loader displays, WebSocket `/meetings` connects (`data-connected="true"`), and single video tile renders labeled `"You"`. Participant counter displays `1`. |
| ROOM-02 | Second participant (member or guest) joins using room code or meeting link. | Both browsers receive WebSocket `participant:joined`, render 2 video tiles, participant counter increments to `2`, and remote peer name displays on remote tile. |
| ROOM-03 | Opening "Participants" sidebar panel (People tab). | Panel lists all active members; Host pinned at the top with `(Host)` badge, self labeled `You`, and co-hosts labeled `(Co-Host)`. |
| ROOM-04 | Using search filter in People panel. | Participant list filters in real-time matching the entered search query. |
| ROOM-05 | Participant leaves meeting via "Leave Meeting" button. | User returns to home (`/`), socket disconnects, and remaining participants receive `participant:left` reducing tile count and updating counter. |
| ROOM-06 | Opening meeting info modal via `ctrl-info` button. | Modal displays meeting title and 8-character hex room code with copy action. |
| ROOM-07 | Switching between sidebar tabs (Chat, People, Agenda, Polls) and closing sidebar. | Sidebar updates to active tab view with matching header; close button (`sidebar-close`) collapses panel. |
| ROOM-08 | Additional participant attempts to join when meeting is full (`maxParticipants` reached). | Server rejects connection with `meeting:full` and disconnects socket; client renders full-screen banner `"This meeting is full"` (`data-end-type="full"`). |
| ROOM-09 | Same user opens meeting room in a second browser tab (duplicate tab). | Earlier tab receives error event `"another tab opened"` and is closed by server; new tab takes over the session. |
| ROOM-10 | Viewing meeting room on mobile viewport (<= 768px). | Layout adapts to mobile responsive shell with mobile header, kebab action menu, and inline action controls (`ctrl-mobile-*`). |

---

## 4. Media & WebRTC (`MED`)

| ID | Action / Condition | Expected Result |
|---|---|---|
| MED-01 | User clicks microphone toggle (`ctrl-mic`) to mute/unmute audio. | Button attribute updates to `data-enabled="true/false"`; remote peers immediately observe muted badge toggle on participant tile via `media:updated`. |
| MED-02 | User clicks camera toggle (`ctrl-camera`) to enable video. | Button attribute updates to `data-enabled="true"`, browser requests camera permissions, and remote peer receives video frames (`videoWidth > 0` on `<video>` element). |
| MED-03 | User disables camera (`ctrl-camera` clicked again). | Local video stream stops, tile reverts to displaying avatar initials, and remote peers stop receiving video track. |
| MED-04 | Participant speaks into microphone (detected by local audio level analyzer). | Client emits `media:speaking { speaking: true }`; other participants see blue border highlight activate on speaker's video tile. |
| MED-05 | User enables Data Saver mode (`ctrl-data-saver`). | Button attribute updates to `data-active="true"`; browser stops rendering remote video streams and displays avatars to conserve bandwidth while audio remains active. |
| MED-06 | User opens Device Settings modal via more options menu (`ctrl-more` -> Settings). | Modal lists available microphones and cameras; Cancel closes without changes; Apply switches active media track. |
| MED-07 | User activates Screen Share. | Browser prompts `getDisplayMedia`, video grid transitions into presentation mode (large screen video in center + participant sidebar). |
| MED-08 | Second participant attempts screen share while another participant is sharing. | System rejects request and alerts that another participant is currently presenting (single presenter limit). |
| MED-09 | Client browser encounters autoplay audio restrictions. | Interstitial overlay "Ready to Join?" appears requiring user interaction to permit media playback. |
| MED-10 | Symmetric NAT scenario blocking direct peer-to-peer connection. | Client uses TURN relay credentials from `GET /webrtc/ice-servers` to relay media via Cloudflare TURN server. |

---

## 5. Collaboration Features (`COL`)

| ID | Action / Condition | Expected Result |
|---|---|---|
| COL-01 | Sending text message in Chat panel (`chat-input` filled and click `chat-send` or Enter). | Other participants receive message in real-time via `chat:message` event with sender name and timestamp; input clears automatically. |
| COL-02 | Attempting to send empty message or whitespace only. | Send button remains disabled; pressing Enter emits no socket event. |
| COL-03 | Registered user joins late to meeting with existing chat history. | Client receives `chat:history` on join and renders all past messages sent by registered users. *(Guest messages are ephemeral and not persisted in database)*. |
| COL-04 | Participant clicks hand raise toggle (`ctrl-hand`). | Button attribute sets `data-active="true"`, hand icon appears on tile across all screens, and name enters queue (`#1`, `#2`) in People panel via `queue:updated`. |
| COL-05 | Participant with raised hand clicks `ctrl-hand` again. | Hand icon disappears from tile and People queue updates automatically. |
| COL-06 | Participant clicks reaction button (`ctrl-reaction`) to send emoji reaction. | Client emits `reaction:send`; all participants receive animated reaction and aggregate counter increments on `reaction-summary`. |
| COL-07 | Host creates agenda item in Agenda tab (`agenda-title-input`, duration, save). | Item saved, form collapses, and all participants receive ordered agenda list in real-time via `agenda:list`. Non-hosts see item list without creation form. |
| COL-08 | Host clicks "Start" on an agenda item. | Item updates to active (`data-active="true"`), "Current Item" card renders at top for all participants via `agenda:active`, and Start button on item is disabled. |
| COL-09 | Host creates poll in Polls tab (question and at least 2 options, then Launch). | All participants receive poll card in real-time via `poll:created` with initial status of 0 total votes. |
| COL-10 | Registered participant votes on a poll option. | Vote recorded, server broadcasts `poll:updated`, and all participants view updated percentage and total vote counter. |
| COL-11 | Participant attempts to vote a second time on the same poll. | System enforces single vote per user (upsert behavior); total vote count does not duplicate. |
| COL-12 | Host attempts to launch poll with empty question. | Poll is not created and form prevents emission. |

---

## 6. Host & Co-Host Controls (`HOST`)

| ID | Action / Condition | Expected Result |
|---|---|---|
| HOST-01 | Host selects "Mute user" (`participant-mute`) on a participant. | Target receives `media:force-mute`, microphone disables (`data-enabled="false"`), and room views updated mute status. |
| HOST-02 | Host selects "Ask to unmute" (`participant-ask-unmute`) on a muted participant. | Target receives confirmation prompt `"Host asks you to unmute"`. If accepted, target mic enables. |
| HOST-03 | Host selects "Remove participant" (`participant-kick`) on a participant. | Target immediately removed, socket disconnected, target screen displays rejection overlay `"You have been removed from the meeting"` (`data-end-type="kicked"`), and room receives `participant:left`. |
| HOST-04 | Host attempts self-kick or regular participant attempts to kick another user. | Server rejects request with error `"Host cannot be kicked"` or `"Unauthorized"`; UI hides moderation menu for regular participants. |
| HOST-05 | Host selects "Make Co-Host" (`participant-make-cohost`) on a registered member. | Target role updates to `CO_HOST` via `participant:role-updated`; label shows `(Co-Host)` and target gains moderation controls. |
| HOST-06 | Host selects "Remove Co-Host" (`participant-remove-cohost`). | Target demoted back to `PARTICIPANT` and moderation permissions revoked. |
| HOST-07 | Host clicks Leave and selects "End Meeting for All" (`leave-end-meeting`). | Server processes `meeting:end`, hard-deletes meeting from database, and emits `meeting:ended`. All participants view overlay `"The host has ended the meeting"` with return home button. |
| HOST-08 | Regular participant clicks Leave in bottom toolbar. | Participant presented only with "Leave Meeting" (`leave-leave-meeting`) without global end meeting option. |
| HOST-09 | All participants leave meeting room (*last participant disconnect*). | Server detects 0 active sockets, triggers termination event, and hard-deletes meeting record from database. |
| HOST-10 | Meeting reaches maximum duration limit (`startedAt + duration`). | Background scheduler sweeps expired meeting, purges database row, and emits `meeting:ended` with reason duration limit reached. |

---

## 7. API Integrity, Security & Platform (`SEC`)

| ID | Action / Condition | Expected Result |
|---|---|---|
| SEC-01 | Accessing protected API endpoint without `Authorization: Bearer <token>` header. | HTTP 401 Unauthorized with standard error envelope: `{ success: false, error: "Unauthorized", statusCode: 401, ... }`. |
| SEC-02 | Browser request originating from unlisted domain origin. | CORS blocks request (omits `Access-Control-Allow-Origin` header). |
| SEC-03 | REST API response structure across all endpoints. | Follows unified envelope standard: success responses include `{ success: true, data: ..., message: "Success", timestamp, path }`; error responses include `{ success: false, error: ..., statusCode, timestamp, path }`. |
| SEC-04 | Submitting payloads with XSS script tags (`<script>alert(1)</script>`) or SQL injection strings in text inputs. | Inputs handled safely: React escapes DOM rendering automatically; Prisma ORM uses parameterized queries preventing SQL injection execution. |
| SEC-05 | Accessing health monitoring endpoint `GET /health`. | Returns HTTP 200 with database status (`db: "up"` or `"down"`), git commit SHA, `uptimeSeconds`, and `testSupport` status. |
| SEC-06 | Accessing test endpoint `POST /test-support/cleanup` without valid admin token. | Returns HTTP 404 Not Found (if test support disabled) or HTTP 401 Unauthorized, protecting against unauthorized data deletion. |
| SEC-07 | Guest token calling meeting creation endpoint `POST /meetings`. | HTTP 403 Forbidden with error indicating guests cannot create meetings. |
