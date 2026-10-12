# BUG-009 — Lowercase Room Code Input Causes Indefinite Joining Screen Hang for Registered Users

| | |
|---|---|
| **Jira Issue** | BC-109 |
| **Spec ID(s)** | `UI-JOIN-02`, `R-17`, `R-18` |
| **Severity** | High |
| **Found in** | Live Deployment (`https://beecollab.vercel.app`, commit `b6fb446`) |
| **Found during** | L4 UI Automation BDD Regression (`@MEET-06`) |

## Summary
When a registered user on the home page inputs a valid room code using lowercase characters (e.g. `8baddb42` instead of `8BADDB42`), the frontend navigates to the meeting URL and gets stuck indefinitely on the full-screen loader (`joining-screen`) with no error feedback, because the backend room code lookup returns 404 and the frontend does not uppercase codes for registered users.

## Steps to Reproduce
1. Log in to `https://beecollab.vercel.app/` as a registered user.
2. In the "Enter a code or link" input field (`join-code-input`), type a valid room code in lowercase (e.g. `8baddb42`).
3. Click "Join" (`join-btn`).
4. Observe the page state.

## Expected (Per Functional Spec)
The frontend should either:
- Automatically normalize the input code to uppercase (`.toUpperCase()`) prior to dispatching the API request (as it currently does for anonymous guest join dialogs), OR
- Display an explicit inline error toast / alert if the code fails to resolve, returning the user to the home screen.

## Actual
The browser displays the full-screen `joining-screen` loader indefinitely.
Network inspection reveals `GET /meetings/code/8baddb42` returned HTTP `404 Not Found`. Because no error handler unmounts the loader, the user is trapped in an unresponsive screen with no way to cancel other than reloading the page.

## Evidence
- Automated test verification: Scenario `@MEET-06` confirmed joining flow fails to connect and room controls never load.
- Network response: `GET https://beecollab-rwbj.onrender.com/meetings/code/8baddb42` -> `{"statusCode": 404, "error": "Not Found"}`.
