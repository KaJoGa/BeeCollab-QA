# BUG-008 — Own Chat Messages Displayed as External Sender with Left-Aligned Layout

| | |
|---|---|
| **Jira Issue** | BC-108 |
| **Spec ID(s)** | `UI-CHAT-01`, `R-24` |
| **Severity** | High |
| **Found in** | Live Deployment (`https://beecollab.vercel.app`, commit `b6fb446`) |
| **Found during** | L4 UI Automation BDD Regression (`@MEET-03`) |

## Summary
When an authenticated user sends an in-call chat message, the message is rendered on the client as if it originated from a third-party participant: it appears left-aligned with the sender's account display name instead of being right-aligned with the identifier "You" and `data-me="true"`.

## Steps to Reproduce
1. Navigate to `https://beecollab.vercel.app/login` and register/login as a user (e.g. "Chat Host").
2. Create and enter a new meeting room.
3. Open the Chat sidebar panel (`ctrl-tab-chat`).
4. Type `"Hello from Chat Host"` into the chat input and click send.
5. Inspect the rendered chat message bubble in the DOM.

## Expected (Per Functional Spec)
Messages sent by the local participant must be styled as outgoing messages:
- Aligned to the right side of the chat panel.
- Header labeled as "You" (or `data-me="true"`).
- Visually distinguishable from incoming messages sent by remote participants.

## Actual
The chat message renders left-aligned with sender name `"Chat Host"`.
Root Cause in code analysis: The frontend helper `isMe` compares `message.senderId` directly against the raw JWT authentication token string rather than the decoded `sub` (user UUID) claim, causing `isMe` to evaluate to `false` for every own message.

## Evidence
- Automated test assertion: `com.beecollab.qa.steps.MeetingSteps.ownChatMessageExhibitsBugR24` verified `isOwnChatMessageMarkedAsYou` returns `false`.
- Domestic message DOM attributes: `data-sender="Chat Host"`, `data-me` omitted/false, flex layout left-aligned (`flex-direction: row`).
