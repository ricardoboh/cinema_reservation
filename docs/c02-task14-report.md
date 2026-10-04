# C02 Task 14 implementation and verification report

Verified locally on 2026-10-04 against Specification Baseline v0.2. Neither
Baseline v0.1 nor v0.2 was modified. No commit or push was performed.

## 1. Gaps found before modification

The repository's runnable implementation was v0.1. Confirm always became
CONFIRMED; availability/allocation queries counted only CONFIRMED; the lifecycle
had only DRAFT/CONFIRMED/CANCELLED. There was no Approver operation or independent
authority, pending policy snapshot, deadline/expiration, rejection or expiration
history/notifications, or owner status read API. Cancellation needed explicit
pending support and terminal-state guards. The existing screening lock and
multi-seat reservation structure could support the extension without redesign.
The narrow dark frontend and its API types/actions knew only the original four
operations; fixtures, manual verification and tests needed extension.

## 2. Files created

Paths below are relative to the repository root. Java package prefix is
`backend/src/main/java/com/reservedbytes/cinema_reservation/`.

- `dto/ApprovalRequest.java`: smallest explicit decision payload.
- `service/ApprovalPolicy.java`: deterministic demo applicability and Approver.
- `service/ExpirationService.java`: locked system expiration transaction.
- `service/ExpirationSweep.java`: lightweight local periodic expiration.
- `frontend/verify-ui.mjs`: dependency-free real-browser CDP verification.
- `docs/c02-task14-report.md`: this report.

Ignored runtime artifacts created under backend/target include
c02-v02-runtime-verification.json, c02-v02-ui-verification.json and screenshots
c02-v02-ui-pending.png, c02-v02-ui-confirmed.png, c02-v02-ui-conflict.png,
c02-v02-ui-expired.png and c02-v02-ui-mobile.png. Maven output remains ignored.

## 3. Files modified

Using the same Java package prefix:

- `CinemaReservationApplication.java`: enable the local sweep.
- `controller/AvailabilityController.java`: resolve system expiration before read.
- `controller/ReservationController.java`: status GET and approval endpoint.
- `dto/ReservationResponse.java`: pending policy, deadline and retained outcomes.
- `dto/SeatAvailability.java`: expose demo approval applicability to the UI.
- `model/Reservation.java`: persist pending snapshot and decision/expiration history.
- `model/ReservationStatus.java`: add PENDING_APPROVAL, REJECTED and EXPIRED.
- `repository/ReservationRepository.java`: pending/confirmed exclusion and due-aware observation queries.
- `service/ReservationService.java`: conditional Confirm, authority, deadlines, terminal guards and notifications.

Other files:

- `backend/src/main/resources/dev-data.sql`: add distinct Approver identity 3.
- `backend/src/test/java/com/reservedbytes/cinema_reservation/ReservationIntegrationTests.java`: extend HTTP, Clock, notification and race verification.
- `backend/src/test/resources/application.yaml`: disable periodic sweep during deterministic tests.
- `backend/verify-baseline.ps1`: assert runtime flows A–F and real deadline behavior.
- `frontend/src/reservations.ts`: real status reads and explicit approval requests.
- `frontend/src/App.tsx`: complete v0.2 booking flow, independent demo Approver and backend polling.
- `frontend/src/App.css`, `frontend/src/index.css`: wider responsive light layout and visible seat/status distinctions.
- `backend/MANUAL-TESTING.md`, `frontend/README.md`, `README.md`: startup, fixtures, API conventions and reproduction.
- `docs/architecture-and-decisions.md`: implementation decisions and exposed C03 drivers.
- `docs/evidence-and-evolution.md`: link this verified implementation evidence.

No dependencies were added and no separate frontend was created.

## 4. Backend behavior

| Operation | Implemented v0.2 result |
|---|---|
| Create | 201 and ID; persisted non-blocking DRAFT; same validation and inclusive 15-minute boundary; no allocation/approval request |
| Availability | 200; CONFIRMED/live PENDING_APPROVAL unavailable; draft/terminal/due holds available; observation query itself read-only |
| Confirm | Owner only, before screening, DRAFT only; whole reservation directly confirmed or pending; pending/confirmed conflicts return 409 with no partial acquisition |
| APPROVE | Authorized identity on live pending only; whole reservation CONFIRMED without releasing/reacquiring seats; actor/decision/time retained |
| REJECT | Same authority/live checks; whole reservation REJECTED, released, decision retained |
| Cancel | Owner only before screening; DRAFT/PENDING_APPROVAL/CONFIRMED become CANCELLED and release all seats; cancellation time retained |
| Expire | Deadline reached becomes EXPIRED; all seats released; effective deadline retained as expiredAt |
| Status read | Owner GET /reservations/{id}; actual persisted state and pending/decision/cancellation/expiration metadata |

Unauthorized decisions return 403 without a requested state/allocation effect.
Invalid decision returns 400. Stale/non-pending decision, repeated cancellation
and terminal-state reopening attempts return 409. Missing references remain 404.
Rejected, expired and cancelled records cannot be reopened. Independent system
expiration can still occur when a late request is rejected.

The existing pessimistic screening lock serializes lifecycle operations through
commit. Pending/confirmed allocations use the same exclusion check. Approve vs
Cancel, Approve vs Reject and stale approval preserve valid sequential outcomes.
All seats share one reservation lifecycle. Approval validates that no conflicting
allocation undermines the hold; invariant failure is rejected for investigation.

The unchanged after-commit Notification Service logs actual CONFIRMED, CANCELLED,
REJECTED and EXPIRED transitions. Pending submission does not send a final
confirmation. No external messaging was added.

## 5. Demo policy and identities

For dev screening 1 / Hall A, IDs 1/2 do not require approval; IDs 3/4 do. The
policy selects physical seat numbers 3/4 for each screening. Any mixed selection
becomes wholly pending. Confirm retains required seat IDs, policy label and
Approver identity for stable delayed decisions.

Customer/owner is user 1; user 2 is another customer; authorized demo Approver
is user 3. Headers are X-User-Id. Owning a reservation alone grants no approval
authority. This is deterministic development identity transport with no login.
The body {decision: APPROVE|REJECT}, status GET and metadata schema are demo
implementation choices, not permanent business/API requirements.

## 6. C02 expiration mechanism

Default duration is configurable PT60S. Deadline is min(request+duration,
screening start); a shorter PT15S override supports quick demonstrations. Clock
is injected and UTC. Approval is strictly before deadline; equality expires.
A simple one-second local sweep resolves idle requests. Pre-read expiration uses
a separate system transaction, leaving OP-02 read-only. Lifecycle operations
resolve expiration and recheck the deadline under the screening lock. Due pending
holds are excluded at observation time. Domain errors commit any system expiration
while leaving the rejected requested transition unapplied. No public expiration
endpoint, distributed scheduler or queue was introduced. Production timeout and
reliable processing design remain unresolved.

## 7. Automated validation

- Backend `mvnw.cmd test`: 60 tests, zero failures/errors/skips.
- Final backend `mvnw.cmd clean package`: BUILD SUCCESS; all 60 tests passed.
- Frontend `npm.cmd run lint`: passed, no warnings.
- Frontend `npm.cmd run build`: passed, TypeScript and Vite bundle generated.
- `git diff --check`: passed.
- Both baseline specifications have no diff.

Tests retain all 37 original test executions and add pending/mixed acquisition,
blocking/conflicts, no partial allocation, both unauthorized decisions, approval,
rejection/cancellation/expiration release, metadata and notifications, strict
59/60/61-second boundaries, deadline capped at screening, due cancellation,
availability-triggered expiration, allocation-triggered expiration, stale decisions
following reacquisition and terminal guards. Repeated races cover pending Confirm
vs Confirm/Cancel, Approve vs Cancel and Approve vs Reject alongside original races.
The test sweep is disabled and the injected Clock is advanced deterministically.

An initial added-test failure was a test-helper POST to the new GET surface (405);
it was corrected. The final test/build results above are from the corrected code.
The previous v0.1 backend initially held the JAR open on Windows; it was stopped
before the successful clean package and restart.

## 8. Actual running API verification

Packaged backend started with dev profile on port 8080; Vite restarted on 5173.
Swagger UI returned 200; OpenAPI 3.1.0 exposed all six paths. The executable script
performed 43 real requests/checks against the default 60-second deadline.

| Flow | Reservation IDs | Actual outcome |
|---|---|---|
| A ordinary | 1 | DRAFT non-blocking -> CONFIRMED unavailable -> CANCELLED released |
| B approve, mixed 1/3 | 2 | PENDING_APPROVAL holds both; owner APPROVE/REJECT 403; Approver 3 APPROVE -> CONFIRMED, both stay unavailable |
| C reject | 3 | PENDING_APPROVAL -> REJECTED; both resources AVAILABLE; stale approval 409 |
| D cancel | 4 | PENDING_APPROVAL -> CANCELLED; both resources AVAILABLE; stale approval 409 |
| E expire | 5 | Real deadline reached -> EXPIRED; both resources AVAILABLE; late approval/cancel 409 |
| F conflict | 6 competing with 5 | Confirm while 5 pending -> 409, remains DRAFT; after expiration can acquire; stale decision on 5 cannot affect 6 |

Evidence: backend/target/c02-v02-runtime-verification.json. Backend logs exposed
Notification Service calls for the actual successful final transitions.

## 9. Frontend and real-browser verification

The existing React/Vite app now uses a wider two-column cinema layout, central
screen/seat map, lighter balanced colors, readable lifecycle badges and retained
outcome/deadline details. It stacks on narrow screens. Selection, availability and
approval applicability come from the backend. Separate demo Approver controls
send identity 3 or allow identity 1 to visibly demonstrate 403. Pending polls the
real status and availability; no local timer fabricates a lifecycle transition.
Every mutation reloads relevant backend data. Both 403 and 409 messages are visible.

Real headless Edge, no mocked responses:

| Flow | UI reservation IDs | Result |
|---|---|---|
| A | 7 | Create/DRAFT still available; direct Confirm/CONFIRMED unavailable; Cancel/released |
| B | 8 | Mixed seats 1/3 both held; owner APPROVE/REJECT visibly 403; authorized APPROVE confirmed without release; cancel cleanup |
| C | 9 | REJECTED visibly retained, seat available |
| D | 10 | Pending CANCELLED visibly retained, seat available |
| F | 11 winner / 12 loser | Two real drafts, pending winner, visible 409 and loser stays DRAFT |
| E | 11 | Actual backend-polled EXPIRED, seat available, expiry time visible; late HTTP approval 409 |

Browser runtime exceptions: none. At 390px width, no horizontal overflow.
Screenshots and request/status evidence are in backend/target/c02-v02-ui-*.
The CDP screenshot helper brings its tab to the foreground because inactive
headless Edge tabs can delay screenshot capture. That is verification tooling,
not application behavior.

## 10. Architectural drivers exposed for C03

Persistent delayed requests and stable policy snapshots; authority separate from
ownership; authoritative time, finite expiration and delayed processing;
concurrency across decisions/cancellation/expiry/acquisition; atomic multi-seat
allocation and hold conversion; stale-decision protection after release/reuse;
observable status and retained outcome history; notifications aligned with
committed transitions. The coarse screening lock and local sweep deliberately
remain simple C02 choices. See architecture-and-decisions.md for evidence and
remaining design pressure. No ideal C03 architecture was implemented.

## 11. Genuine remaining TBDs

Production criteria for requiring approval and scoped authority assignment;
production finite timeout/deadline policy; production approval/status schemas and
trusted identity transport; future abandoned-DRAFT TTL. Reliable persistent
scheduling/time coordination, allocation enforcement/recovery at scale, audit/read
representation and durable notification delivery/retry are C03 design decisions.
Pending blocking, terminal outcomes, owner/Approver distinction, deadline equality
and atomic allocation are resolved v0.2 behavior, not remaining TBDs.
