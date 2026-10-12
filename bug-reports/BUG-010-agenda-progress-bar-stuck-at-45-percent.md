# BUG-010 — Agenda "Current Item" Progress Bar is Hard-Coded at 45% and Never Advances

| | |
|---|---|
| **Jira Issue** | BC-110 |
| **Spec ID(s)** | `UI-AGENDA-03`, `R-26` |
| **Severity** | Low |
| **Found in** | Live Deployment (`https://beecollab.vercel.app`, commit `b6fb446`) |
| **Found during** | L4 UI Automation Code Inspection & Regression |

## Summary
In the meeting room Agenda sidebar panel, when an agenda item is marked as active and started, the visual progress bar in the "Current Item" section is hard-coded with an inline CSS style of `width: 45%`. As elapsed time increases, the progress bar remains permanently static at 45% without dynamic calculation based on item duration.

## Steps to Reproduce
1. Enter an active meeting room as host.
2. Open the Agenda panel (`ctrl-tab-agenda`).
3. Create an agenda item with a duration (e.g. 10 minutes) and save.
4. Click "Start" on the agenda item.
5. Inspect the "Current Item" progress bar DOM element.

## Expected (Per Functional Spec)
The progress bar width must dynamically represent elapsed time relative to the agenda item's configured duration (`width: (elapsed / totalDuration) * 100%`), advancing smoothly over the meeting course.

## Actual
The progress bar element has a static hard-coded attribute `style="width: 45%;"` rendered directly from JSX template markup. It never updates or advances.

## Evidence
- JSX inspection in frontend meeting agenda component:
  ```tsx
  <div className="bg-primary h-2 rounded-full" style={{ width: '45%' }} />
  ```
- Visual verification: progress bar remains motionless across multiple minutes of meeting runtime.
