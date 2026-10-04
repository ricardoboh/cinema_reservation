# C02 reservation demo

Run the existing backend and frontend in separate terminals from the repository root.
Java 21 or newer and Node/npm are required. No authentication or new dependencies
are introduced.

Backend:

```powershell
cd backend
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=dev"
```

Frontend:

```powershell
cd frontend
npm.cmd ci
npm.cmd run dev
```

Open http://127.0.0.1:5173. The demo uses user ID 1 and screening ID 1.
The displayed movie/hall description comes from the known development fixtures;
there is no screening-info endpoint. Seat IDs, rows, numbers and availability
come from the API, not a hardcoded seat list. Screening 1 starts one day after dev
backend initialization. Restart the backend to reset fixtures and relative times.

## Demonstrate all four operations

1. The page loads availability. Click **Check Availability** to refresh manually.
2. Select available seats (labels show row:seat, e.g. 1:3). Unavailable seats are
   marked in red, labelled Unavailable, and disabled.
3. Click **Create Reservation**. A real HTTP 201 supplies the reservation ID;
   the baseline guarantees DRAFT. The ID, status and reserved seats are shown.
   Availability is reloaded and seats remain available because DRAFT does not hold them.
4. Click **Confirm Reservation**. A real HTTP 200 supplies CONFIRMED. Availability
   is reloaded: reserved seats become UNAVAILABLE.
5. Click **Cancel Reservation**. A real HTTP 200 supplies CANCELLED. Availability
   is reloaded: seats become AVAILABLE and selectable again.

Cancel is also available directly on a DRAFT. Confirm is shown only for DRAFT;
Cancel only for DRAFT/CONFIRMED. The current reservation is retained while active:
finish or cancel it before creating another. Reloading the page clears the UI's
current reservation, but does not cancel/delete the backend record.

## Demonstrate a visible backend error

1. Open two tabs. In each, select the same available seat and create a DRAFT.
2. Confirm in the first tab.
3. Confirm in the second tab. Its error alert shows the backend's message:
   `A selected seat is already confirmed for this screening. (HTTP 409)`.
   The second reservation stays DRAFT and availability refresh shows the seat unavailable.
4. Cancel both reservations to clean up the demo.

Errors show backend Problem Detail text plus HTTP status. Failed transitions do
not fabricate a new state. Controls are disabled during requests. If availability
cannot reload, stale seats cannot be selected; Check Availability retries.
Timeouts do not prove a state-changing request failed; verify its outcome through
Swagger before retrying an uncertain request.

## API mapping

| UI action | Backend operation |
|---|---|
| Initial load / Check Availability | GET /screenings/1/availability |
| Create Reservation | POST /reservations with userId 1, screeningId 1, selected seatIds |
| Confirm Reservation | POST /reservations/{id}/confirm with X-User-Id: 1 |
| Cancel Reservation | POST /reservations/{id}/cancel with X-User-Id: 1 |

Vite proxies both /reservations and /screenings to http://localhost:8080.
No backend CORS changes are required. This proxy is for the development server;
a deployed build would need an equivalent same-origin proxy.

## Validation

```powershell
npm.cmd run lint
npm.cmd run build
cd ../backend
.\mvnw.cmd test
```

Verified on 2026-10-04 in a real headless Edge browser against the running Vite
and Spring Boot dev servers, without mocked responses:

- Reservation 4, seats 3 and 4: GET 200 (AVAILABLE), Create 201 (DRAFT; still
  AVAILABLE), Confirm 200 (CONFIRMED; UNAVAILABLE/disabled), Cancel 200
  (CANCELLED; AVAILABLE/selectable).
- Two-tab conflict: reservations 5 and 6 drafted seat 3; Confirm 5 returned 200;
  Confirm 6 returned 409 and visibly displayed the backend message, retaining
  DRAFT. Both cancellations returned 200 during cleanup.
- No browser runtime exceptions. Frontend lint/build passed; all 37 backend
  tests passed with no failures, errors or skips.

Local captures are in backend/target/c02-ui-flow.png,
backend/target/c02-ui-error.png and backend/target/c02-ui-verification.json.
The target directory is ignored and cleared by a clean build.
