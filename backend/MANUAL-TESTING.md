# C02 v0.2 development demonstration

Source of truth: [Baseline v0.2](../docs/c02_baseline_specification_v0.2.md).
Both baseline specifications are preserved. These fixtures, headers, payloads and
local timeout are implementation choices for C02, not new business requirements.

## Start and validate

Java 21 or newer and Node/npm are required. In separate terminals:

```powershell
cd backend
.\mvnw.cmd test
.\mvnw.cmd clean package
java -jar target/cinema-reservation-0.0.1-SNAPSHOT.jar --spring.profiles.active=dev
```

```powershell
cd frontend
npm.cmd ci
npm.cmd run lint
npm.cmd run build
npm.cmd run dev
```

Open http://127.0.0.1:5173 and http://localhost:8080/swagger-ui/index.html.
OpenAPI is http://localhost:8080/v3/api-docs. Stop the previous backend before
clean/package on Windows because a running JAR is locked. Vite uses port 5173
and proxies /reservations and /screenings to port 8080.

## Demo policy and identity

The disposable dev H2 database resets on restart. Screening 1 starts one day
later. Hall A has four seats, IDs 1–4, row 1, numbers 1–4:

| Resource in screening 1 | Demo policy |
|---|---|
| Seats 1 and 2 | No approval; Confirm goes directly to CONFIRMED |
| Seats 3 and 4 | Approval required; Confirm goes to PENDING_APPROVAL |
| Mixed selection, e.g. 1 and 3 | Whole reservation pending, both seats held |

The simple policy checks seat numbers 3/4, independently of screening. Confirm
snapshots required seat IDs, policy label and authorized Approver ID on the
pending record. A delayed decision uses that snapshot rather than recalculating
policy. Policy administration is outside C02.

Owner/customer: user 1. Another customer: user 2. Authorized demo Approver:
user 3. Dev fixtures contain all three identities. Confirm, Cancel and owner
status reads use `X-User-Id: 1`; approval uses `X-User-Id: 3`. These are explicit
caller identity conventions with no authentication. Owning a reservation does
not confer authority; identity 3 is the separately assigned demo authority.

Screening 2 starts ten minutes later (Create cutoff has passed). Screening 3
started an hour earlier. Reservation 100 is a draft and 101 is confirmed for
screening 3; neither can validly confirm/cancel after screening start.

## API choices and observable history

| Operation | Request / response |
|---|---|
| Create | POST /reservations with userId, screeningId, seatIds; 201 and id |
| Status | GET /reservations/{id}, owner header; 200 with persisted status and history |
| Availability | GET /screenings/{screeningId}/availability; AVAILABLE/UNAVAILABLE and demo approvalRequired flag |
| Confirm | POST /reservations/{id}/confirm, owner header; 200 CONFIRMED or PENDING_APPROVAL |
| Cancel | POST /reservations/{id}/cancel, owner header; 200 CANCELLED |
| Decide | POST /reservations/{id}/approval, Approver header, `{"decision":"APPROVE"}` or `{"decision":"REJECT"}`; 200 CONFIRMED or REJECTED |

The status GET, flag, header and minimal decision body are C02 representation
choices; production schemas and identity transport remain C03 TBDs. A malformed
decision returns 400, missing record 404, unauthorized caller 403, and invalid,
repeated or stale lifecycle operation 409. Failed decisions cannot reopen a
terminal record or modify a later reservation's allocation.

Responses retain creation/cancellation times, approvalRequestedAt,
approvalDeadline, approvalPolicy, approvalRequiredSeatIds,
authorizedApproverId, decision, decidedBy, decidedAt and expiredAt. Approved
then cancelled reservations retain both decision and cancellation information.

## Finite expiration

Default demo duration: **60 seconds**, configurable with
`--demo.approval-timeout=PT15S` for a faster demo. Deadline is the earlier of
request time + demo duration and screening start. This value is not a permanent
business rule. DRAFT has no TTL and never holds seats.

The injected UTC Clock is authoritative. At or after the deadline, decisions
and pending cancellation fail with 409 and the record becomes EXPIRED. expiredAt
records the effective deadline, not the later processing time. There is no public
expire endpoint and the UI never changes lifecycle locally.

A simple in-process sweep runs every second. Status/availability controllers
also resolve due expiration through a separate locked transaction before the
read-only query. Availability filters pending holds by the observation time, so
even a hold becoming due between sweep and query cannot block indefinitely.
Confirm/Cancel/approval resolve due expiration under the same screening lock.
The mutation transactions intentionally commit system expiration on a domain
error response (noRollbackFor ReservationException); the rejected requested
transition has no effect. Unauthorized decisions check authority before any
mutation. Notifications happen after commit only.

No distributed scheduler, queue or external messaging is introduced. The local
sweep is best effort; requests enforce correctness even if it is delayed. Tests
disable the sweep and advance a fixed test Clock without waiting.

## Reproduce flows A–F in the frontend

1. Check Availability; the seat map loads real backend state. Green seats are
   available, purple selected, muted red unavailable. Labels identify policy.
2. **A:** select seat 1, Create Reservation: DRAFT and ID visible, seat still
   AVAILABLE. Confirm Reservation: CONFIRMED, seat UNAVAILABLE. Cancel to release.
3. **B:** select seats 1 and 3, Create then Confirm: whole reservation
   PENDING_APPROVAL, both UNAVAILABLE, request/deadline visible. In the separate
   DEMO Approver section select Approver 3 and APPROVE: CONFIRMED, both remain
   unavailable. Cancel to release. Selecting Owner 1 for a decision instead
   demonstrates HTTP 403 with unchanged pending state/hold.
4. **C:** select 3, Create, Confirm, REJECT as Approver 3: REJECTED, seat AVAILABLE.
5. **D:** select 3, Create, Confirm, Cancel as owner: CANCELLED, seat AVAILABLE.
6. **E:** select 3, Create, Confirm; leave pending until the visible deadline.
   Backend polling shows EXPIRED and AVAILABLE. A late approval through Swagger
   returns 409. The UI disables decisions on terminal outcomes.
7. **F:** in two tabs select seat 3 and create overlapping DRAFTs. Confirm one:
   PENDING_APPROVAL. Confirm the other: HTTP 409 visibly displayed, loser stays
   DRAFT. Cancel both for cleanup.

Cancel also works on DRAFT and CONFIRMED before screening. Terminal records stay
visible; create a new reservation for another attempt. Every mutation reloads
status and availability; pending polls every second. Page reload clears the UI's
current reservation but does not delete/cancel backend records. Requests can time
out after succeeding: refresh/check status before retrying an uncertain mutation.
Released availability assumes no subsequent reservation has acquired that seat.

## Executable runtime verification

On a fresh dev instance, from backend:

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -Command "& './verify-baseline.ps1' | ConvertTo-Json -Depth 8 | Set-Content target/c02-v02-runtime-verification.json"
```

The script exercises A–F, both unauthorized decisions, mixed holds, all release
paths and stale approval after reacquisition, and checks Swagger and six OpenAPI
paths. It waits for the actual backend deadline; no state is seeded or faked.
For browser verification, start a headless Chromium/Edge with debugging port
9222, then run `node frontend/verify-ui.mjs` from the repository root. This uses
Node's built-in WebSocket and CDP, adds no frontend dependencies, and writes
ignored screenshots/network evidence to backend/target.
