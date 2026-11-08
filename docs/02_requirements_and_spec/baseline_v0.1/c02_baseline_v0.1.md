# Specification Baseline v0.1 — Cinema Reservation System

**Team:** Deadlock  
**Domain:** Cinema Seat Reservation System  
**Baseline Version:** v0.1  
**Status:** Frozen Baseline Specification

---

## 1. Shared Domain Rules & Invariants

Domain rules are centrally defined in this section and explicitly referenced by individual operations.

### BR-01 — Resource Scope & Discrete Slot Semantics

- A **Resource** is formally defined as the unique discrete pair `(Screening, Seat)`, representing a physical seat assigned to a specific movie screening.
- Every `Screening` possesses a fixed UTC start time (`startTime`).
- Seat availability is strictly evaluated relative to a specific `Screening`. Physical seats across different screenings are independent resources.

### BR-02 — Exclusive Resource Invariant (No Double Booking)

For every discrete `(screeningId, seatId)` pair across all valid system states, the following invariant must strictly hold:

$$\text{count}(\text{CONFIRMED reservations}) \le 1$$

- Reservations in `DRAFT` or `CANCELLED` states do **not** block or allocate resources.

### BR-03 — Domain-Specific Creation Horizon

A new reservation draft (`DRAFT`) can only be created at least **15 minutes prior to the scheduled screening start time**:

$$t_{\text{current}} \le t_{\text{screening\_start}} - 15\text{ minutes}$$

- A creation request submitted exactly 15 minutes before the start time is allowed.
- Creation requests submitted less than 15 minutes before the start time are rejected (`409 Conflict`).
- **Rationale:** Limits last-minute creation to streamline cinema box-office and admission operations.

### BR-04 — Cancellation Policy

- A reservation can only be cancelled by its registered owner.
- Eligible source states for cancellation are `DRAFT` and `CONFIRMED`.
- Cancellation is permitted strictly **before** the screening start time:

$$t_{\text{current}} < t_{\text{screening\_start}}$$

- Cancellation attempts at or after the screening start time are rejected.
- Repeated cancellation attempts on a reservation in `CANCELLED` state are rejected with `409 Conflict`.
- Cancellation does not delete the reservation record from persistent storage.

### BR-05 — DRAFT and Resource Allocation

- The `DRAFT` state represents a customer's non-binding intent and does **not** allocate resources or block seat availability.
- `DRAFT` reservations do not restrict seats in `Check Availability`.
- Multiple users may simultaneously hold `DRAFT` reservations containing overlapping seats.
- Exclusive allocation is established solely upon successful execution of `Confirm Reservation`.
- If two conflicting `DRAFT` reservations attempt confirmation, at most one can successfully transition to `CONFIRMED`.

### BR-06 — Confirm / Cancel Concurrency

If `Confirm Reservation` and `Cancel Reservation` are executed concurrently against the same `DRAFT` reservation, the system must process them to yield exactly one valid sequential state:

- If `Confirm` executes successfully first, the state becomes `CONFIRMED`; subsequent `Cancel` may cancel it if BR-04 holds, resulting in `CANCELLED`.
- If `Cancel` executes successfully first, the state becomes `CANCELLED`; concurrent or subsequent `Confirm` is rejected.

---

## 2. Specification of Baseline Operations

### OP-01 — Create Reservation

**Goal / User Value:**  
A registered customer creates a non-binding reservation proposal (`DRAFT`) for one or more seats in a specific screening without allocating them.

**Trigger:**  
Registered user submits `POST /reservations` containing the target screening and seat references.

**Observable Requirement(s):**

**REQ-01:** The system creates one new reservation in `DRAFT` state if the user, screening, and selected seats exist, all seats belong to the screening's hall, and BR-03 is satisfied.

Create performs **no allocation conflict check**, as `DRAFT` does not constitute a binding resource allocation under BR-05.

**Preconditions:**

- User exists;
- Screening exists;
- `seatIds` list is non-empty;
- All specified seats exist and belong to the hall assigned to the screening;
- $t_{\text{current}} \le t_{\text{screening\_start}} - 15\text{ minutes}$.

**Success Postcondition:**

- Exactly one new reservation with a unique `reservationId` exists;
- `Reservation.state = DRAFT`;
- The reservation links the specified user, screening, and seats;
- Seats are not blocked or reserved by this action.

**State Transition:**  
$[\text{none}] \rightarrow \text{DRAFT}$

**Referenced Domain Rules:**  
BR-01, BR-03, BR-05.

#### Main Success Scenario

1. User submits `userId`, `screeningId`, and a non-empty list of `seatIds`.
2. System verifies existence of user and screening.
3. System verifies existence of seats and their hall membership.
4. System verifies creation horizon rule BR-03.
5. System creates a reservation in `DRAFT` state.
6. System persists the reservation.
7. System returns `HTTP 201 Created` with the generated reservation ID.

#### Alternative / Error Outcomes

- Non-existent user $\rightarrow$ `404 Not Found`;
- Non-existent screening $\rightarrow$ `404 Not Found`;
- Non-existent seat $\rightarrow$ `404 Not Found`;
- Seat does not belong to screening's hall $\rightarrow$ `400 Bad Request`;
- Empty or invalid seat list $\rightarrow$ `400 Bad Request`;
- Screening starts in less than 15 minutes $\rightarrow$ `409 Conflict`.

#### Verification Examples

- Screening in 2 hours + valid seats A1, A2 $\rightarrow$ `201 Created`, single `DRAFT` created;
- Screening in exactly 15 minutes $\rightarrow$ `201 Created`;
- Screening in 10 minutes $\rightarrow$ `409 Conflict`;
- Seat already `CONFIRMED` + valid request $\rightarrow$ `DRAFT` creation succeeds, but subsequent `Confirm` will fail upon collision.

---

### OP-02 — Check Availability

**Goal / User Value:**  
A user inspects which seats are available for a specific movie screening.

**Trigger:**  
User requests seat availability for a specific `screeningId`.

**Observable Requirement(s):**

**REQ-02:** For an existing screening, the system marks a seat as `UNAVAILABLE` if the seat for that screening is part of a `CONFIRMED` reservation. Otherwise, it is marked as `AVAILABLE`.

**Preconditions:**

- Screening exists.

**Success Postcondition:**

- System returns seat availability structure;
- No domain or reservation state is altered.

**State Transition:**  
None.

**Referenced Domain Rules:**  
BR-01, BR-02, BR-05.

#### Main Success Scenario

1. Client requests seat availability for `screeningId`.
2. System verifies screening existence.
3. System fetches seats belonging to the screening's hall.
4. For each seat, system checks if a `CONFIRMED` reservation exists for `(Screening, Seat)`.
5. System returns seats marked as `AVAILABLE` or `UNAVAILABLE` with `HTTP 200 OK`.

#### Alternative / Error Outcomes

- Non-existent screening $\rightarrow$ `404 Not Found`.

---

### OP-03 — Confirm Reservation

**Goal / User Value:**  
A valid `DRAFT` reservation transitions into a binding allocation, guaranteeing the selected seats to the customer.

**Trigger:**  
Reservation owner submits `POST /reservations/{id}/confirm`.

**Observable Requirement(s):**

**REQ-03:** System confirms a `DRAFT` reservation only if none of its seats belong to another `CONFIRMED` reservation for the same screening.

**REQ-04:** During concurrent confirmation attempts for overlapping reservations, at most one reservation may transition to `CONFIRMED`. All competing requests must be rejected with `409 Conflict`.

**Preconditions:**

- Reservation exists;
- Caller is the reservation owner;
- `Reservation.state = DRAFT`;
- $t_{\text{current}} < t_{\text{screening\_start}}$.

**Success Postcondition:**

- `Reservation.state = CONFIRMED`;
- Seats are exclusively allocated for the screening;
- BR-02 is preserved;
- Confirmation notification is initiated via `Notification Service`.

**State Transition:**  
$\text{DRAFT} \rightarrow \text{CONFIRMED}$

**Referenced Domain Rules:**  
BR-01, BR-02, BR-05, BR-06.

#### Main Success Scenario

1. User submits confirmation request for `reservationId`.
2. System verifies reservation existence and ownership.
3. System verifies state is `DRAFT` and screening has not started.
4. System validates all `(Screening, Seat)` pairs against BR-02.
5. In the absence of conflicts, system atomically updates state to `CONFIRMED`.
6. System initiates confirmation notification.
7. System returns `HTTP 200 OK` with confirmed reservation details.

#### Alternative / Error Outcomes

- Non-existent reservation $\rightarrow$ `404 Not Found`;
- Caller is not owner $\rightarrow$ `403 Forbidden`;
- Reservation not in `DRAFT` $\rightarrow$ `409 Conflict`;
- Screening already started $\rightarrow$ `409 Conflict`;
- At least one seat already confirmed by another reservation $\rightarrow$ `409 Conflict`;
- Concurrent conflicting confirmation $\rightarrow$ at most one succeeds, others return `409 Conflict`.

---

### OP-04 — Cancel Reservation

**Goal / User Value:**  
A customer cancels their reservation prior to the screening start, releasing allocated seats back to the available pool.

**Trigger:**  
Reservation owner submits `POST /reservations/{id}/cancel`.

**Observable Requirement(s):**

**REQ-05:** System allows the owner to cancel a reservation in `DRAFT` or `CONFIRMED` state if the screening has not started.

**REQ-06:** Successfully cancelled reservation transitions to `CANCELLED`, remains stored, and stops blocking resource availability.

**REQ-07 (Confirm/Cancel Concurrency):** If `Confirm` and `Cancel` execute concurrently on the same `DRAFT` reservation, the system must enforce valid sequential ordering under BR-06.

**Preconditions:**

- Reservation exists;
- Caller is the reservation owner;
- $\text{Reservation.state} \in \{\text{DRAFT}, \text{CONFIRMED}\}$;
- $t_{\text{current}} < t_{\text{screening\_start}}$.

**Success Postcondition:**

- `Reservation.state = CANCELLED`;
- Reservation record is retained;
- Reservation stops blocking seats in `Check Availability`;
- Cancellation notification is initiated via `Notification Service`.

**State Transition:**  
$\text{DRAFT} \rightarrow \text{CANCELLED}$ or $\text{CONFIRMED} \rightarrow \text{CANCELLED}$

**Referenced Domain Rules:**  
BR-02, BR-04, BR-05, BR-06.

#### Main Success Scenario

1. User submits cancellation request for `reservationId`.
2. System verifies existence and ownership.
3. System verifies state is `DRAFT` or `CONFIRMED`.
4. System verifies screening has not started.
5. System updates state to `CANCELLED` and records cancellation timestamp.
6. System initiates cancellation notification.
7. System returns `HTTP 200 OK` with reservation in `CANCELLED` state.

#### Alternative / Error Outcomes

- Non-existent reservation $\rightarrow$ `404 Not Found`;
- Caller is not owner $\rightarrow$ `403 Forbidden`;
- Reservation already `CANCELLED` $\rightarrow$ `409 Conflict`;
- Screening already started $\rightarrow$ `409 Conflict`.

---

## 3. Reservation State Model (v0.1)

```text
[none]
   |
   | Create Reservation (OP-01)
   v
 DRAFT
  |  \
  |   \ Cancel (OP-04)
  |    \
  |     v
  |  CANCELLED
  |
  | Confirm (OP-03)
  v
CONFIRMED
  |
  | Cancel (OP-04)
  v
CANCELLED
```

`CANCELLED` is a terminal state in v0.1. Operations attempted on invalid source states are rejected without altering the reservation state.

---

## 4. REST API Mapping

| Operation | HTTP Method | Endpoint | Success Status |
| --- | --- | --- | --- |
| **Check Availability** | `GET` | `/screenings/{screeningId}/availability` | `200 OK` |
| **Create Reservation** | `POST` | `/reservations` | `201 Created` |
| **Confirm Reservation** | `POST` | `/reservations/{id}/confirm` | `200 OK` |
| **Cancel Reservation** | `POST` | `/reservations/{id}/cancel` | `200 OK` |

---

## 5. Requirements Acceptance Review

| Criterion | Resolution in Baseline v0.1 |
| --- | --- |
| **Clarity of Meaning** | Resource is explicitly defined as `(Screening, Seat)`. `DRAFT`, `CONFIRMED`, and `CANCELLED` states are fully specified. |
| **User Value** | Every operation defines its user goal and role within the reservation process. |
| **Observable Outcome** | Requirements specify observable outcomes, seat states, and response codes without dictating framework implementations. |
| **Feasibility** | `DRAFT` does not allocate resources, whereas `CONFIRMED` allocates exclusively; requirements coexist without contradiction. |
| **Verifiability** | Every operation includes concrete verification scenarios and test inputs. |
| **Time Boundaries** | Boundaries $\le t_{\text{start}} - 15\text{m}$ and $< t_{\text{start}}$ are strictly defined. |
| **Concurrency** | REQ-04 resolves conflicting confirmations; REQ-07 resolves concurrent Confirm/Cancel races. |
| **Consistency** | All operations adhere to shared rules BR-01 through BR-06 and share one state model. |

---

## 6. Accepted Baseline v0.1 Decisions

1. `DRAFT` does not block seats; Create performs no binding collision check.
2. Only `CONFIRMED` reservations block seat availability in v0.1.
3. Creation is allowed up to exactly 15 minutes before screening start.
4. Binding availability checks occur exclusively during Confirm.
5. Two conflicting reservations cannot both be `CONFIRMED`.
6. Cancellation is permitted strictly before `screening.startTime`.
7. Repeated cancellation returns `409 Conflict`.
8. Cancellation sets state to `CANCELLED` without deleting persistent records.
9. `Notification Service` is an external system boundary triggered post-commit.
10. Automatic TTL for abandoned `DRAFT` reservations remains an open TBD for future baselines.