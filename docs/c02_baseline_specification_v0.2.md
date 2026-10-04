# C02 — Baseline Specification v0.2: Cinema Reservation System

**Team:** Deadlock  
**Domain:** Cinema Seat Reservation System  
**Baseline Version:** v0.2  
**Previous baseline:** [Specification Baseline v0.1](c02_baseline_specification.md), preserved unchanged.  
**Change source:** [Accepted C02 change-impact analysis](c02-change-impact-analysis.md).

This is a specification only. API mappings and diagrams below describe required behavior; they do not assert that the application implements v0.2. Existing requirement and rule identifiers retain their identity. The approval decisions supplied for Task 13 are incorporated below. Additional lifecycle clarifications made in this baseline are identified in section 6; unresolved policy parameters are listed at the end.

## 1. Shared Domain Rules & Invariants

Domain rules are defined here and referenced by the operations.

### BR-01 — Resource Scope & Discrete Slot Semantics

Unchanged: a Resource is the pair `(Screening, Seat)`, representing a physical seat for one screening. Every Screening has a `startTime`. Availability is evaluated for that specific screening, not for the physical seat across all screenings.

### BR-02 — Exclusive Resource Invariant (No Double Booking)

For every `(screeningId, seatId)` in every valid system state:

`count(reservations containing the Resource with state in {PENDING_APPROVAL, CONFIRMED}) <= 1`

This strengthens, and preserves, the v0.1 guarantee `count(CONFIRMED reservations) <= 1`. A pending hold excludes both another pending hold and another confirmed allocation. `DRAFT`, `CANCELLED`, `REJECTED` and `EXPIRED` do not block Resources.

Acquiring, converting or releasing a reservation's allocation is atomic across all its seats. A conflicting request cannot acquire a subset. Approval converts the reservation's own hold; that hold is not a conflict with itself. Availability is an observation, not a promise that a later Confirm will succeed.

### BR-03 — Domain-Specific Creation Horizon

Unchanged: Create is allowed when `currentTime <= screening.startTime - 15 minutes`. Exactly 15 minutes before the screening is allowed; later creation is rejected. This limits last-minute creation and supports cinema admission. This is a creation constraint, not a new confirmation or approval cutoff.

### BR-04 — Cancellation Policy

- Only the reservation owner may cancel.
- Eligible source states are `DRAFT`, `PENDING_APPROVAL` and `CONFIRMED`.
- Cancellation requires `currentTime < screening.startTime`; at or after the start it is rejected.
- Cancellation moves the entire reservation to `CANCELLED`, releases any hold/allocation and retains the record and cancellation time.
- Repeated cancellation, or cancellation from `REJECTED` or `EXPIRED`, is rejected with `409 Conflict`.

Only the additional source state changes the v0.1 policy; ownership, time boundary and history retention remain unchanged.

### BR-05 — DRAFT and Resource Allocation

`DRAFT` is a customer's intention and is non-blocking. Multiple drafts may contain the same Resource. Create does not check allocation conflicts or initiate approval.

The exclusive allocation boundary remains successful Confirm: it acquires either a final allocation (`CONFIRMED`) or a temporary hold (`PENDING_APPROVAL`). Approval is required to turn the latter into a final allocation. While a competing allocation persists, at most one conflicting draft can successfully leave `DRAFT` through Confirm.

### BR-06 — Lifecycle Concurrency

Changes must have outcomes equivalent to a valid sequential ordering, with no partial state/allocation updates.

- Confirm/Cancel on one draft: if Cancel succeeds first, Confirm is rejected. If Confirm succeeds first, the state becomes `CONFIRMED` or `PENDING_APPROVAL`; Cancel may then succeed under BR-04.
- Approval/Cancel on a pending reservation: if Cancel succeeds first, approval is rejected. If approval succeeds first, Cancel may subsequently cancel `CONFIRMED` under BR-04.
- Approval/rejection: only one decision can leave `PENDING_APPROVAL`; the later decision is rejected.
- Approval/expiration: approval is allowed only before the deadline in BR-08. Once expiration is due, no approval may resurrect the hold, even if expiration processing was delayed.
- Rejection/expiration/cancellation: only the first valid transition from pending succeeds. Later requests are rejected for their resulting source state. No terminal state is overwritten.
- Conflicting Confirm requests must preserve BR-02. After release, a new Confirm may acquire the Resources; an old delayed approval cannot affect that new allocation.

### BR-07 — Approval Applicability and Authority

Some Resources require approval and others do not. Confirm evaluates that policy for the reservation's Resources. If none require approval, Confirm goes directly to `CONFIRMED`. If at least one requires approval, the whole reservation enters `PENDING_APPROVAL` and holds all its seats. There is no partial confirmation or per-seat lifecycle in v0.2.

An **Authorized Approver** is a new actor authorized to decide approval for the relevant Resources. Being the owner alone does not confer that authority. A positive decision covers all approval-required Resources in the reservation; otherwise the reservation cannot become `CONFIRMED`. The policy used for the pending request is retained so delayed decisions have a stable meaning. Policy administration and changing an existing pending request's policy are outside this baseline.

### BR-08 — Pending Deadline, Release and Terminal Outcomes

Each pending request has an observable request time and a finite `approvalDeadline`, no later than `screening.startTime`. Approval or rejection requires `currentTime < approvalDeadline` and `currentTime < screening.startTime`. At `currentTime >= approvalDeadline`, the request expires and moves to `EXPIRED`, releasing every held Resource. There is no indefinite pending hold.

Expiration is a system time-driven outcome, not an Approver action. Processing must ensure a due hold does not remain unavailable to a subsequent availability observation or allocation attempt. OP-02 itself remains read-only; coordinating expiration with observations and mutations is a C03 responsibility. Exact timeout duration and processing mechanism remain TBD; verification uses an explicit deadline.

Before the deadline, delayed approval leaves the reservation `PENDING_APPROVAL` and its hold intact. Rejection moves it to `REJECTED`; owner cancellation moves it to `CANCELLED`. All three release paths retain the reservation and outcome history. `REJECTED`, `EXPIRED` and `CANCELLED` are terminal in v0.2: no retry, reapproval, reopening or return to `DRAFT` exists. A new attempt uses a new reservation subject to the unchanged creation horizon.

### BR-09 — Observable Approval Progress and Notifications

The owner must be able to observe the reservation state, pending request time/deadline and decision or expiration outcome/time. The decision record identifies the authorized decision-maker. Exact representation and read API remain TBD; these are observable requirements, not storage prescriptions.

The existing Notification Service boundary remains. A final confirmation notification is initiated only when the reservation actually enters `CONFIRMED`, either in OP-03 or OP-05. Pending submission must not be reported as final confirmation. Successful cancellation continues to initiate its notification. Rejection and expiration initiate outcome notifications. Notification delivery mechanics and retry strategy belong to C03 and do not change allocation semantics.

## 2. Specification of Baseline Operations

## OP-01 — Create Reservation

**Goal / user value:** A registered customer creates one `DRAFT` reservation for one or more seats in a screening, without allocating them. Behavior is unchanged from v0.1.

**Trigger:** Registered user submits `POST /reservations` with selected screening and seats.

**Observable requirement(s):**

**REQ-01 (unchanged):** Create one new `DRAFT` when the user, screening and seats exist, all selected seats belong to the screening's hall and BR-03 holds. Create performs no allocation conflict check, including against pending holds, and starts no approval request.

**Preconditions:** User and screening exist; `seatIds` is nonempty and valid; seats exist and belong to the screening's hall; `currentTime <= screening.startTime - 15 minutes`.

**Success postcondition:** Exactly one new reservation with a unique `reservationId`, selected owner, screening and seats exists in `DRAFT`. It blocks no seats.

**State change:** `[none] -> DRAFT`.

**Referenced domain rules / invariants:** BR-01, BR-03, BR-05.

### Main success scenario

1. User submits `userId`, `screeningId` and nonempty `seatIds`.
2. System validates user, screening, seats and their hall membership.
3. System checks BR-03.
4. System creates and stores one `DRAFT`.
5. System returns `201 Created` and its identifier.

### Alternative / failure outcomes

- Missing user, screening or seat: `404 Not Found`; no reservation created.
- Empty/invalid seat list or seat outside the screening's hall: `400 Bad Request`; no reservation created.
- Less than 15 minutes before screening: `409 Conflict`; no reservation created.

### Verification examples

- Valid A1/A2, screening in two hours: `201 Created`, one non-blocking `DRAFT`.
- Exactly 15 minutes before screening: `201 Created`; ten minutes before: `409 Conflict`.
- Missing seat: `404 Not Found`.
- Resource already `CONFIRMED` or `PENDING_APPROVAL`: otherwise valid Create still succeeds; subsequent Confirm fails while the competing allocation persists.

**Rationale:** Create records intent; allocation and approval applicability belong to Confirm. Approval does not justify changing creation validation or its time boundary.

**Assumption / unknown / TBD:** Future automatic TTL for abandoned drafts remains the v0.1 TBD. No draft expiration is introduced in v0.2; drafts remain non-blocking.

## OP-02 — Check Availability

**Goal / user value:** User observes which seats are available for one screening.

**Trigger:** User requests availability for `screeningId`.

**Observable requirement(s):**

**REQ-02 (updated):** For an existing screening, mark a seat `UNAVAILABLE` if its Resource belongs to a `CONFIRMED` or unexpired `PENDING_APPROVAL` reservation; otherwise mark it `AVAILABLE`. Due expiration must be reflected according to BR-08.

**Preconditions:** Screening exists.

**Success postcondition:** Return seat availability without this operation changing reservation or domain state. The result reflects a coherent observation under BR-02 and BR-08, and does not reserve seats.

**State change:** None.

**Referenced domain rules / invariants:** BR-01, BR-02, BR-05, BR-08.

### Main success scenario

1. Client requests availability for the screening.
2. System verifies screening existence and obtains its hall's seats.
3. System observes active allocations, with due expiration reflected under BR-08.
4. System returns `200 OK` and each seat as `AVAILABLE` or `UNAVAILABLE`.

### Alternative / failure outcomes

- Missing screening: `404 Not Found`.

### Verification examples

- No blocking reservations: all seats `AVAILABLE`.
- A1 in `CONFIRMED` or unexpired `PENDING_APPROVAL`: A1 `UNAVAILABLE`.
- A2 only in drafts or terminal reservations: A2 `AVAILABLE`.
- After rejection, expiration or cancellation, the released seat is `AVAILABLE` provided no new reservation has acquired it.

**Rationale:** A pending hold must prevent competing allocation during approval delay. The query remains read-only, and the Resource scope and response vocabulary are unchanged.

## OP-03 — Confirm Reservation

**Goal / user value:** Owner submits a draft for exclusive allocation: direct confirmation where approval is unnecessary, or a temporarily held approval request where it is required.

**Trigger:** Owner submits `POST /reservations/{id}/confirm`.

**Observable requirement(s):**

**REQ-03 (updated):** A valid draft with no competing pending/confirmed allocation becomes `CONFIRMED` if no Resource requires approval, or `PENDING_APPROVAL` if any Resource requires approval. All seats are allocated atomically in either branch.

**REQ-04 (updated):** Concurrent conflicting Confirm requests preserve BR-02; while the successful allocation remains active, at most one conflicting request succeeds and the others receive `409 Conflict`. This protects temporary holds as well as final allocations.

**Preconditions:** Reservation exists; caller is its owner; state is `DRAFT`; `currentTime < screening.startTime`; none of its Resources is held/allocated by another reservation. Approval policy is determinable and the required pending deadline can be assigned under BR-08.

**Success postcondition:** Either `CONFIRMED` with all seats allocated and final confirmation notification initiated, or `PENDING_APPROVAL` with all seats temporarily held and request time/deadline observable. BR-02 holds. Pending submission is not final confirmation.

**State change:** `DRAFT -> CONFIRMED` (no approval required) or `DRAFT -> PENDING_APPROVAL` (approval required).

**Referenced domain rules / invariants:** BR-01, BR-02, BR-05, BR-06, BR-07, BR-08, BR-09.

### Main success scenario

1. Owner submits the confirmation request.
2. System verifies existence, ownership, draft state and pre-screening time.
3. System evaluates approval applicability for all Resources.
4. System checks allocation conflicts and atomically acquires all seats.
5. If no approval is required, system stores `CONFIRMED` and initiates final confirmation notification.
6. Otherwise, system stores `PENDING_APPROVAL`, its applicable policy, request time and finite deadline, retaining a hold on all seats.
7. System returns `200 OK` with the actual resulting reservation state and pending metadata when applicable.

### Alternative / failure outcomes

- Missing reservation: `404 Not Found`.
- Caller is not owner: `403 Forbidden`.
- State is not `DRAFT`, screening has started, or any seat has a competing pending/confirmed allocation: `409 Conflict`; this request performs no state/allocation change.
- Concurrent conflicting request loses acquisition: `409 Conflict`; no partial seat hold. A separate successful concurrent cancellation may have changed the reservation state under BR-06.

### Verification examples

- Free Resources, none require approval, screening not started: `200 OK`, `CONFIRMED`, confirmation notification initiated.
- At least one Resource requires approval: `200 OK`, `PENDING_APPROVAL`, deadline observable, every selected seat unavailable; no final confirmation notification.
- Competing unexpired pending or confirmed allocation: `409 Conflict`, draft unchanged absent another lifecycle action.
- Two conflicting drafts confirmed concurrently: at most one active pending/confirmed allocation, losing request `409 Conflict`.
- Confirm on `CANCELLED`, `REJECTED`, `EXPIRED` or already pending: `409 Conflict`.

**Rationale:** Confirm remains the owner's allocation request and collision boundary, while a separate authorized decision provides final confirmation for approval-required Resources.

## OP-04 — Cancel Reservation

**Goal / user value:** Owner withdraws an unwanted reservation before screening, releasing final allocation or temporary hold while retaining history.

**Trigger:** Owner submits `POST /reservations/{id}/cancel`.

**Observable requirement(s):**

**REQ-05 (updated):** Owner may cancel `DRAFT`, `PENDING_APPROVAL` or `CONFIRMED` before screening under BR-04.

**REQ-06 (unchanged):** Successful cancellation retains the reservation in `CANCELLED` and stops its blocking of Resources.

**REQ-07 (extended):** Concurrent Confirm/Cancel and pending decision/Cancel/expiration preserve the valid sequential outcomes in BR-06, with no partial update or resurrection.

**Preconditions:** Reservation exists; caller is owner; state is `DRAFT`, `PENDING_APPROVAL` or `CONFIRMED`; `currentTime < screening.startTime`. A pending request whose expiration is already due is resolved under BR-08 and is no longer cancellable.

**Success postcondition:** Reservation is `CANCELLED`, record and cancellation time retained, all its holds/allocations released, cancellation notification initiated.

**State change:** `DRAFT -> CANCELLED`, `PENDING_APPROVAL -> CANCELLED` or `CONFIRMED -> CANCELLED`.

**Referenced domain rules / invariants:** BR-02, BR-04, BR-05, BR-06, BR-08, BR-09.

### Main success scenario

1. Owner submits cancellation.
2. System verifies existence and ownership.
3. System checks eligible state, pending deadline where applicable, and pre-screening time.
4. System atomically stores `CANCELLED`, cancellation time and release of all its seats.
5. System initiates cancellation notification and returns `200 OK` with `CANCELLED`.

### Alternative / failure outcomes

- Missing reservation: `404 Not Found`; non-owner: `403 Forbidden`.
- Terminal reservation, repeated Cancel, or screening at/after start: `409 Conflict`; no cancellation change.
- Due pending expiration or concurrent decision: source state is re-evaluated under BR-06/BR-08; cancellation cannot overwrite `EXPIRED` or `REJECTED`. Cancellation after successful approval remains possible before screening.

### Verification examples

- Owner cancels any eligible state before screening: `200 OK`, `CANCELLED`.
- Pending cancellation before deadline: all held seats released; late approval gets `409 Conflict`.
- Exactly at screening start: `409 Conflict`; cancellation performs no transition.
- Repeated Cancel: `409 Conflict`; non-owner: `403 Forbidden`.
- Concurrent Confirm/Cancel or approval/Cancel: only sequentially valid transitions; no leaked hold.

**Rationale:** The accepted ownership and time policy also permits withdrawal during approval delay. Record retention and cancellation notification remain unchanged.

## OP-05 — Approve Reservation

**Goal / user value:** Authorized Approver makes an independent decision on a pending request, granting final reservation certainty or rejecting it and releasing Resources. This operation has its own actor, authority, preconditions and observable outcome; it is not an internal step of Create or an automatic success of Confirm.

**Trigger:** Authorized Approver submits an approval decision for a pending reservation, proposing `POST /reservations/{id}/approval` with `decision = APPROVE` or `REJECT`.

**Observable requirement(s):**

**REQ-08 (new):** Only an Approver authorized for all relevant approval-required Resources may approve or reject a live pending request. Unauthorized decisions receive `403 Forbidden` and have no decision/allocation effect.

**REQ-09 (new):** An authorized positive decision on a live pending request atomically changes it to `CONFIRMED`, retains exclusive allocation of all seats and initiates final confirmation notification.

**REQ-10 (new):** An authorized negative decision atomically changes a live pending request to `REJECTED`, releases all seats, retains the outcome and initiates rejection notification.

**REQ-11 (new, system expiration obligation):** A pending request expires at its deadline, becomes `EXPIRED`, releases all seats and initiates expiration notification. Expiration requires no Approver call and prevents later approval. This is a lifecycle obligation referenced here, not another human success branch.

**REQ-12 (new):** Delayed approval remains observable as `PENDING_APPROVAL` until a valid decision, cancellation or deadline. Request/deadline and outcome metadata are observable under BR-09.

**Preconditions:** Reservation exists and is `PENDING_APPROVAL`; caller has BR-07 authority; decision is valid; `currentTime < approvalDeadline` and `currentTime < screening.startTime`; the reservation holds all selected Resources exclusively under BR-02.

**Success postcondition:** Positive decision: reservation `CONFIRMED`, allocation maintained without release/reacquisition, decision-maker/time recorded, final confirmation notification initiated. Valid negative decision: reservation `REJECTED`, all holds released, decision-maker/time recorded, rejection notification initiated. Both retain reservation history and satisfy BR-02.

**State change:** `PENDING_APPROVAL -> CONFIRMED` for APPROVE; `PENDING_APPROVAL -> REJECTED` for REJECT. System expiration separately performs `PENDING_APPROVAL -> EXPIRED`.

**Referenced domain rules / invariants:** BR-01, BR-02, BR-06, BR-07, BR-08, BR-09.

### Main success scenario

1. Approver submits APPROVE for a reservation identifier.
2. System verifies reservation existence and the caller's authority for the relevant Resources.
3. System verifies live pending state, deadline and pre-screening time.
4. System verifies the reservation's own exclusive hold and protects the decision against concurrent lifecycle changes.
5. System atomically records the decision and changes the whole reservation to `CONFIRMED`, preserving its allocation.
6. System initiates final confirmation notification.
7. System returns `200 OK` with the confirmed reservation and observable decision outcome.

### Alternative / failure outcomes

- Authorized REJECT with the same live-request checks: atomically record rejection, move to `REJECTED`, release all seats, initiate rejection notification, return `200 OK` with `REJECTED`. This is a valid business outcome, not an HTTP error.
- Missing reservation: `404 Not Found`.
- Caller lacks relevant Approver authority, including an owner without that authority: `403 Forbidden`; no decision effect.
- Invalid decision value: `400 Bad Request`; no decision effect.
- Non-pending state, repeated decision, or stale decision after cancellation/rejection/expiration: `409 Conflict`; no decision effect.
- At/after deadline: `409 Conflict` for approval/rejection; system expiration must be reflected under BR-08. A delayed expiration worker cannot authorize late approval.
- Competing lifecycle transition wins: re-evaluate state and reject invalid decision with `409 Conflict`.
- Exclusive hold cannot be validated: no confirmation or partial allocation is permitted; treat as an invariant failure requiring investigation, rather than overriding another reservation. Recovery mechanism belongs to C03.

### Verification examples

- Authorized APPROVE before deadline: `200 OK`, `CONFIRMED`; seats remain unavailable, final confirmation notification initiated.
- Owner without Approver authority submits APPROVE or REJECT: `403 Forbidden`, live pending state/hold unchanged by the attempt.
- Authorized REJECT before deadline: `200 OK`, `REJECTED`, seats available unless subsequently reallocated.
- No decision before deadline: pending remains observable and blocks; at deadline: `EXPIRED`, hold released, expiration observable and notified.
- Approval at deadline or after expiration: `409 Conflict`; no resurrection.
- Approval after owner cancellation: `409 Conflict`, `CANCELLED` retained.
- Two Approvers decide concurrently: one transition leaves pending; the other receives `409 Conflict`.

**Rationale:** Delayed, authorized human decision has independent engineering meaning and observable failure/outcome semantics. Separating it from Confirm prevents unauthorized or premature final confirmation and protects the pending hold.

**Assumption / unknown / TBD:** Authority assignment/scope and the exact finite approval timeout remain policy TBDs. They do not relax the authorization check, terminal outcomes or deadline boundary specified above.

## 3. Reservation State Model

The complete v0.2 transition set is:

```text
[none]           -- Create ------------------------------------> DRAFT
DRAFT            -- Confirm [no Resource requires approval] ---> CONFIRMED
DRAFT            -- Confirm [any Resource requires approval] --> PENDING_APPROVAL
DRAFT            -- owner Cancel [before screening] -----------> CANCELLED
CONFIRMED        -- owner Cancel [before screening] -----------> CANCELLED
PENDING_APPROVAL -- authorized APPROVE [live request] ----------> CONFIRMED
PENDING_APPROVAL -- authorized REJECT [live request] -----------> REJECTED
PENDING_APPROVAL -- deadline reached --------------------------> EXPIRED
PENDING_APPROVAL -- owner Cancel [live, before screening] ------> CANCELLED
```

| State | Meaning | Blocks Resources? | Terminal in v0.2? |
|---|---|---|---|
| `DRAFT` | Intention; neither approved nor allocated | No | No |
| `PENDING_APPROVAL` | Submitted request awaiting decision; finite exclusive hold | Yes, until resolution/deadline | No |
| `CONFIRMED` | Final reservation; required approval has succeeded, if applicable | Yes | No; pre-screening cancellation remains allowed |
| `REJECTED` | Authorized negative decision retained in history | No | Yes |
| `EXPIRED` | Pending deadline reached without an earlier valid resolution | No | Yes |
| `CANCELLED` | Owner withdrawal retained in history | No | Yes |

All guards include the shared rules. No direct approval of `DRAFT`, no `CONFIRMED -> EXPIRED/REJECTED`, no `PENDING_APPROVAL -> DRAFT` and no outgoing terminal transitions are introduced. Rejected operations perform no requested transition; independent due expiration may still occur. No TTL transition for drafts is specified.

## 4. REST/API Mapping — Specification Only

| Operation | Method | Endpoint | Successful observable result |
|---|---|---|---|
| Check Availability | `GET` | `/screenings/{screeningId}/availability` | `200 OK`; pending and confirmed allocations unavailable |
| Create Reservation | `POST` | `/reservations` | `201 Created`; `DRAFT` |
| Confirm Reservation | `POST` | `/reservations/{id}/confirm` | `200 OK`; `CONFIRMED` or `PENDING_APPROVAL` explicitly identified |
| Cancel Reservation | `POST` | `/reservations/{id}/cancel` | `200 OK`; `CANCELLED` |
| Approve Reservation (including rejection decision) | `POST` | `/reservations/{id}/approval` (new proposed mapping) | `200 OK`; `CONFIRMED` or `REJECTED` |

Existing paths and success codes remain; Confirm's returned state must distinguish request acceptance from final confirmation. Approval is a separate proposed operation endpoint; exact payload schema is deferred to C03. Expiration is automatic, with no public expiration endpoint required. A read surface must expose REQ-12/BR-09; its endpoint/schema is TBD because v0.1 specifies no reservation-status read endpoint. No endpoint is implemented by this document.

## 5. Requirements Acceptance Review

| Criterion | v0.2 specification result |
|---|---|
| Meaning | Resource unchanged; all six states and blocking semantics explicit |
| User value | Each operation states its goal and rationale; Approver decision is independent |
| Observable outcome | State, availability, decisions, deadline and outcome times specified |
| Feasibility | Non-blocking drafts coexist; only one pending/confirmed allocation per Resource |
| Verification | Operation examples and cross-operation examples below cover both confirmation paths and every release path |
| State/time | Original creation/cancellation boundaries preserved; finite pending deadline has a strict approval boundary |
| Concurrency | REQ-04/REQ-07 and BR-06 cover acquisition, conversion and release races |
| Consistency | Operations, API results and diagrams share BR-01 through BR-09 and one transition set |
| Uncertainty | Policy parameters and C03 mechanisms explicitly separated from required outcomes |

## 6. Accepted Baseline Decisions

1. Preserve non-blocking Create/DRAFT and the exact 15-minute creation boundary.
2. Pending approval temporarily blocks; confirmed reservations continue to block.
3. Confirm checks exclusive allocation and branches by Resource approval applicability.
4. Approval is an independent authorized actor operation; rejection is its negative business decision.
5. Pending cancellation uses the existing owner-only, strictly-before-screening policy.
6. Rejection, expiration and cancellation release all holds and preserve history.
7. Final confirmation and cancellation retain the Notification Service boundary; rejection/expiration add outcome notification needs.
8. Future draft TTL remains unresolved and introduces no behavior in this baseline.

The following are explicit v0.2 consistency clarifications of the accepted lifecycle, rather than claims that the analysis selected implementation details:

9. `REJECTED` and `EXPIRED` are terminal, as is `CANCELLED`; retries require a new reservation.
10. A reservation containing any approval-required Resource is handled as one atomic pending request holding all its seats.
11. Every pending request has a finite deadline no later than screening start; approval at the deadline is invalid. The actual timeout value remains TBD.
12. Policy applicable to a pending request is retained; no policy-edit or partial-decision transitions are added.

## C02 Change Impact — Baseline v0.2

### Changed condition

Some `(Screening, Seat)` Resources require an authorized person's approval before a reservation becomes `CONFIRMED`. The decision may be delayed, negative or absent until expiration. Resources not requiring approval retain direct confirmation.

### Affected requirements / specification sections

- REQ-02 / OP-02: pending holds join confirmed allocations in availability.
- REQ-03/REQ-04 / OP-03: conditional confirmation and exclusive pending acquisition.
- REQ-05/REQ-07 / OP-04: pending cancellation and broader lifecycle concurrency.
- New REQ-08 through REQ-12 / OP-05: authority, positive/negative decisions, expiration and observable delay/outcome.
- BR-02, BR-04, BR-05, BR-06: strengthen exclusion and extend eligible states/ordering; new BR-07 through BR-09 specify approval policy, deadline and observability.
- Sections 3–6: extended lifecycle, API decision mapping, acceptance review and baseline decisions.
- Use cases, state diagrams, verification and C03 drivers described below.

### Unaffected requirements / sections and why

- REQ-01 / OP-01: same validation, draft result, identity and creation horizon; approval applies at allocation, not creation.
- BR-01: screening-specific seat identity remains adequate for the new approval condition.
- BR-03: approval changes confirmation, not the cinema's creation horizon.
- REQ-06: cancellation still retains a non-blocking `CANCELLED` record.
- Existing cancellation ownership, strict pre-screening boundary and repeated-cancellation conflict remain appropriate during approval delay.
- Drafts remain non-blocking and may overlap; availability remains a read-only observation with the same AVAILABLE/UNAVAILABLE vocabulary.
- Direct confirmation without approval, existing API paths/codes, history retention and final-confirmation/cancellation notification boundary remain applicable.
- The original no-double-booking guarantee is preserved and strengthened to cover temporary holds; no implementation technology is selected.

### New actor / operation

Add Authorized Approver and OP-05 Approve Reservation, including its REJECT outcome. Owner operations remain Create, Check Availability, Confirm and Cancel. The owner does not automatically acquire approval authority. Time-driven expiration is a system responsibility rather than a human actor goal.

### Changed rules / meaning of states

Confirm now starts either final allocation or a finite pending hold. `PENDING_APPROVAL` is exclusive but not a final promise of confirmation. Positive approval preserves that allocation; negative decision, expiration and cancellation release it. Terminal `REJECTED`/`EXPIRED` record distinct observable outcomes rather than overloading `CANCELLED`. BR-02 continues to prevent duplicate confirmed allocations.

### Use-case diagram changes

Retain User associations with Create Reservation, Check Availability, Confirm Reservation and Cancel Reservation, and the existing Notification Service system boundary. Add Authorized Approver associated with **Approve/Reject Reservation** (OP-05). Annotate Confirm's conditional pending outcome and Cancel's eligibility for pending reservations. Do not model human approval as a mandatory synchronous `include` of Confirm: it can occur later or never. Annotate system expiration and outcome notifications; expiration does not require an Approver actor. Final confirmation notification is associated with actual entry into `CONFIRMED`, including OP-05, and not pending submission.

### State-diagram changes

Retain initial creation and the existing `DRAFT -> CONFIRMED`, `DRAFT -> CANCELLED`, `CONFIRMED -> CANCELLED` edges, with the direct confirmation edge guarded by no approval-required Resource. Add `PENDING_APPROVAL`, `REJECTED`, `EXPIRED` and exactly five new edges represented in section 3: `DRAFT -> PENDING_APPROVAL` and the four outgoing pending transitions. Guard positive/negative decisions by authority and live deadline; guard pending cancellation by ownership and time. Mark rejected, expired and cancelled states terminal. Add no retry or partial confirmation edges.

### New verification examples

Unless stated otherwise, screening is at 20:00, creation is before 19:45, request is at 19:00, and the test fixture supplies `approvalDeadline = 19:10`. This deadline is a fixture, not the selected production timeout. Availability after release assumes no intervening acquisition.

| ID | Action / condition | Required observable result |
|---|---|---|
| V-01 | Confirm free A1 requiring no approval | `200`, `CONFIRMED`; A1 unavailable; final confirmation notification initiated |
| V-02 | Confirm free A2 requiring approval | `200`, `PENDING_APPROVAL`; request/deadline visible; A2 unavailable; no final confirmation notification |
| V-03 | Observe V-02 at 19:05 with no decision | Still pending, A2 unavailable; delay distinguishable from rejection/expiration |
| V-04 | Authorized APPROVE at 19:06 | `200`, `CONFIRMED`; allocation never released; actor/time observable |
| V-05 | Unauthorized APPROVE or REJECT while live | `403`; no decision or hold change |
| V-06 | Authorized REJECT at 19:06 | `200`, terminal `REJECTED`; A2 available; outcome retained and notification initiated |
| V-07 | No decision; time reaches 19:10 | Terminal `EXPIRED`; A2 available; expiration time observable and notification initiated |
| V-08 | Owner cancels pending at 19:06 | `200`, terminal `CANCELLED`; A2 available; cancellation notification initiated |
| V-09 | Conflicting drafts submit Confirm concurrently, with approval required | While acquired hold persists, at most one `PENDING_APPROVAL`; loser `409`; no overlapping pending/confirmed allocation |
| V-10 | Another draft attempts Confirm against active pending hold | `409`; loser stays draft; after release it may succeed if its guards still hold |
| V-11 | Two positive/negative decisions race on one pending request | One valid decision wins; other `409`; no overwritten outcome or leaked hold |
| V-12 | Approval races owner cancellation before deadline | Cancel first: later approval `409`; approval first: owner may subsequently cancel confirmed reservation; BR-02 holds throughout |
| V-13 | Approval at 19:10 while expiration processing is delayed | `409`; deadline outcome is `EXPIRED`, Resources released; processing delay cannot permit confirmation |
| V-14 | Release then another draft acquires A2; old approval arrives | Old decision `409`; terminal old record preserved; new allocation unaffected |
| V-15 | Reservation has free normal A1 and approval-required A2 | Entire reservation pending and both unavailable; rejection/expiration/cancellation releases both |
| V-16 | Multi-seat draft includes one seat held by another reservation | Confirm `409`; no partial hold on otherwise free seats |
| V-17 | Pending expiration races cancellation or rejection | First valid pre-deadline resolution wins; if deadline is reached first, `EXPIRED`; never two terminal outcomes |

### Architectural drivers for C03

- Persistent pending request, stable approval applicability, deadline, decision authority and outcome history.
- Authorization of the Approver across relevant Resources, separate from owner cancellation/confirmation authority.
- Delayed processing across requests with observable status, request time and outcome time.
- Reliable finite expiration, authoritative time handling and release even when processing is delayed; timer/scheduler is a candidate, not a selected mechanism.
- Atomic multi-seat allocation and pending-to-confirmed conversion without a release window.
- Conflict handling across Confirm, approval, rejection, cancellation, expiration and subsequent reservations; stale requests must not resurrect allocations.
- Final-confirmation, cancellation, rejection and expiration notifications aligned with successful lifecycle outcomes.
- API representation of conditional confirmation and independent decision, plus a reservation status read surface.

## 7. Final Consistency Review and v0.1 → v0.2 Record

### Consistency review

| Reviewed relationship | Finding |
|---|---|
| Requirements vs domain rules | REQ-01 retains BR-01/03/05; REQ-02–04 use strengthened BR-02; REQ-05–07 use BR-04/06; REQ-08–12 use BR-07–09. No requirement permits overlapping pending/confirmed allocations. |
| Operation semantics | Create is non-blocking; Confirm owns acquisition; OP-05 owns authorized decision; Cancel and system expiration release. Rejection is a successful business decision, distinct from an unauthorized HTTP failure. |
| State transitions | All operation outcomes appear in section 3; terminal states have no outgoing edges. Direct confirmation remains possible only without required approval. No draft TTL or reopen transition is implied. |
| Availability semantics | Only live pending and confirmed reservations block. Every pending terminal path releases all seats. OP-02 stays read-only; due expiration must be resolved for observations/allocation under BR-08. |
| Verification examples | V-01–17 cover both confirmation paths, delay, authority, all release paths, deadline equality, mixed seats and conflicting operations. Released availability is qualified against subsequent acquisition. |
| Proposed diagrams | Use-case associations separate owner request from delayed Approver decision; state-diagram guards/edges match the complete textual model. Expiration is system-driven. |

### 1. What changed from v0.1

Conditional Confirm; exclusive temporary pending hold; Authorized Approver and independent OP-05; approval/rejection decision outcomes; automatic finite pending expiration; pending cancellation; terminal `REJECTED`/`EXPIRED`; stronger allocation/concurrency rules; observable approval progress and outcome metadata; rejection/expiration notification needs; updated API specification and diagram guidance; new verification examples and C03 drivers.

### 2. What did NOT change from v0.1 and why

Resource identity, Create validation and DRAFT result, non-blocking overlapping drafts, exact 15-minute creation boundary, owner-only cancellation strictly before screening, cancellation history and repeated-cancel rejection, direct confirmation for ordinary Resources, the original no-double-booking guarantee, read-only availability vocabulary, existing endpoint paths/success codes and final-confirmation/cancellation notification boundary. These rules still serve the original cinema workflow; approval affects allocation lifecycle rather than resource identity, creation eligibility or ownership. v0.1 itself remains preserved as a separate document. Application code is outside this task.

### 3. Remaining genuine TBDs

- Exact pending timeout/deadline calculation within BR-08's finite, no-later-than-screening constraint.
- Business criteria/configuration selecting approval-required Resources and assigning Approver authority; the mandatory per-request authorization behavior is already specified.
- Reservation status read API and exact approval/status payload schemas, to be designed in C03 without changing the required observable outcomes.
- Existing future draft TTL question; v0.2 introduces no draft expiration.

Persistence, scheduling, transaction/concurrency technique, time coordination, invariant-failure recovery and notification delivery/retry mechanisms are C03 design decisions, not unresolved permission to weaken v0.2 requirements. Whether rejection/expiration are terminal, whether pending blocks, and whether pending is cancellable are resolved here.

### 4. Architectural drivers to carry into C03

Carry forward persistent delayed approval, scoped authorization, finite reliable expiration, observable status/history, atomic exclusive multi-seat allocation, valid concurrent lifecycle ordering, stale-decision protection, outcome notifications and API representation of request versus final confirmation. Select mechanisms during C03; implement none during this specification task.
