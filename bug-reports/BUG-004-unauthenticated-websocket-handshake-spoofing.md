# BUG-004 — WebSocket Handshake Accepts Invalid JWT Tokens Allowing Unauthenticated Speaking Event Spoofing

| | |
|---|---|
| **Jira Issue** | `Local/Internal` (or `BC-104`) |
| **Spec ID(s)** | `WS-SEC-01`, `WS-SEC-02` (Related to `R-09`) |
| **Severity** | **Critical** (S1 - Authentication Bypass / Event Spoofing) |
| **Found in** | Live Deployment (`wss://beecollab-rwbj.onrender.com/meetings`), backend commit `b6fb446` |
| **Found during** | Layer 3 WebSocket Real-Time Security Audit (`tools/test-ws-bugs.mjs`) |

## Summary
The Socket.io gateway namespace `/meetings` fails to reject connections initialized with invalid, forged, or garbage JWT tokens during the handshake phase. While missing tokens are rejected, any non-empty string (even `totally-invalid-garbage-jwt-token-xyz`) is permitted to establish an active WebSocket connection. Furthermore, unguarded real-time events such as `media:speaking` do not verify socket authentication or room membership, allowing completely unauthenticated clients to broadcast speaking indicators to real meeting rooms.

## Steps to Reproduce
1. Connect a WebSocket client to `wss://beecollab-rwbj.onrender.com/meetings` with handshake credentials:
   `auth: { token: "totally-invalid-garbage-jwt-token-xyz" }`.
2. Observe that the socket connection succeeds (`s.connected === true`).
3. Obtain a valid meeting UUID (`meetingId`) currently active on the platform.
4. From the unauthenticated socket, emit `media:speaking` with payload `{"meetingId": "<UUID>", "speaking": true}`.
5. In another browser or socket connected as a legitimate meeting host/participant, observe incoming events.

## Expected (Per Functional Spec & Security Standards)
Any WebSocket handshake providing an unparseable or unverified JWT token must be rejected during the connection lifecycle with an authorization error and immediate socket closure. All broadcast events must be guarded by verifying that the sending socket belongs to the target room.

## Actual
- Socket connection is accepted without error (assigned valid `socketId`).
- The unauthenticated socket successfully emits `media:speaking`.
- Legitimate meeting participants receive the `media:speaking` event and display a speaking border for an unknown/spoofed participant.

## Evidence
Log output from `tools/test-ws-bugs.mjs`:
```
[R-09 Check] Socket with garbage token connected: true (socketId=46eZkxLTd1F3P2P4AAAC)
PASS  Handshake allows garbage JWT token to connect (R-09 verified)
[R-09 Check] Host received media:speaking from unauthenticated socket: true
PASS  Unauthenticated socket can spoof media:speaking in room (R-09 verified)
```

## Root Cause & Impact
In `signaling.gateway.ts`, the connection lifecycle middleware checks `if (!token) throw new Error("No token provided")` but does not execute `jwtService.verify(token)` inside the connection hook. Token validation is only applied downstream to specific guarded events, leaving unguarded gateway events vulnerable to arbitrary external emission.
