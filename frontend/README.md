# C02 v0.2 React/Vite demo

See [the full development and manual guide](../backend/MANUAL-TESTING.md) for
fixtures, API choices, deadline behavior, startup commands and flows A–F.

```powershell
npm.cmd ci
npm.cmd run lint
npm.cmd run build
npm.cmd run dev
```

Open http://127.0.0.1:5173 with the backend dev profile on port 8080. Vite proxies
both API paths. The light, wide layout keeps the screen and seat map central and
puts booking state and the separate DEMO Approver controls alongside it. Narrow
screens use a single column. Seat availability and approval policy come from the
backend; movie/hall prose describes known dev fixtures.

Customer 1 creates/requests/cancels; Approver 3 decides. Seats 1/2 confirm directly;
3/4 require approval; mixed selections hold all seats. The Approver caller selector
also allows Owner 1 to demonstrate a real 403. Two tabs with competing drafts
show a real 409. Pending state polls status and availability every second until a
backend terminal/confirmed outcome is observed. Countdown is display-only and
never determines a transition. A terminal booking permits a fresh draft.

No new dependencies, authentication, catalogue, payment or separate frontend.
No transitions are mocked: Create retrieves its backend record and every mutation
reloads status/availability. Stale availability disables selection until a
successful refresh. Page reload does not cancel the backend booking.

Browser verification: from the repository root, `node frontend/verify-ui.mjs`
with Chromium/Edge listening on local debugging port 9222. Evidence is written to
ignored backend/target files. The script uses existing Node capabilities only.
