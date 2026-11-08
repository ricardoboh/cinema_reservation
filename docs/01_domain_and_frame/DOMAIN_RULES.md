# Shared Domain Rules & Invariants — Cinema Reservation System

**Team:** Deadlock  
**Domain:** Cinema Seat Reservation System  
**Baseline Alignment:** Baseline v0.1 & Baseline v0.2 Evolution (C01–C05)

---

## 1. Domain Invariants (Core Rules)

The domain rules defined in this document represent the authoritative invariants of the system. All operational workflows, API endpoints, and architectural components must strictly enforce these rules.

---

### BR-01 — Resource Scope & Discrete Slot Semantics

- A **Resource** is formally defined as the unique discrete pair `(Screening, Seat)`, representing a specific physical seat reserved for a single movie screening.
- Every `Screening` possesses an immutable UTC start time (`startTime`).
- Seat availability, allocations, and holds are strictly evaluated within the scope of a specific `Screening`. Physical seats across different screenings are completely independent resources.

---

### BR-02 — Exclusive Resource Invariant (No Double Booking)

For every discrete `(screeningId, seatId)` pair across all committed system states, the following invariant must strictly hold:

$$\text{count}(\text{reservations with state} \in \{\text{PENDING\_APPROVAL}, \text{CONFIRMED}\}) \le 1$$

- A reservation in `CONFIRMED` or `PENDING_APPROVAL` state holds an exclusive allocation over all its associated seats.
- Reservations in `DRAFT`, `CANCELLED`, `REJECTED`, or `EXPIRED` states do **not** block resources.
- Resource acquisition, hold conversion, and release are **atomic** across all seats within a reservation. A multi-seat reservation cannot acquire a partial subset of its requested seats.

---

### BR-03 — Domain-Specific Creation Horizon

A new reservation draft (`DRAFT`) can only be created at least **15 minutes prior to the scheduled screening start time**:

$$t_{\text{current}} \le t_{\text{screening\_start}} - 15\text{ minutes}$$

- A creation request submitted exactly 15 minutes before the start time is allowed.
- A creation request submitted less than 15 minutes before the start time is rejected (`409 Conflict`).
- **Rationale:** This rule guarantees operational time for cinema staff and box-office admission workflows.

---

### BR-04 — Cancellation Policy

- **Ownership:** A reservation can only be cancelled by its registered owner.
- **Eligible States:** Cancellation is permitted for reservations in `DRAFT`, `PENDING_APPROVAL`, or `CONFIRMED` states.
- **Time Boundary:** Cancellation is allowed strictly **before** the screening start time:

$$t_{\text{current}} < t_{\text{screening\_start}}$$

- At or after the screening start time, cancellation requests are rejected (`409 Conflict`).
- **Idempotency & Guard:** Repeated cancellation attempts, or cancellation of reservations in `REJECTED` or `EXPIRED` states, are rejected with `409 Conflict`.
- **Effect:** Successful cancellation moves the reservation to `CANCELLED`, releases all associated seat holds/allocations, and retains the historical record and timestamp.

---

### BR-05 — DRAFT and Resource Allocation

- The `DRAFT` state represents a customer's non-binding intention and does **not** allocate resources or block seat availability.
- Multiple users may simultaneously hold `DRAFT` reservations containing overlapping seats.
- The exclusive allocation boundary is enforced during the `Confirm` operation:
  - If no selected seats require approval $\rightarrow$ transitions directly to `CONFIRMED`.
  - If any selected seat requires approval $\rightarrow$ transitions to `PENDING_APPROVAL` and places a temporary hold on all seats.
- If two conflicting `DRAFT` reservations attempt to confirm overlapping seats concurrently, at most one can succeed.

---

### BR-06 — Lifecycle Concurrency & Sequential Integrity

System state transitions must produce observable outcomes equivalent to a valid sequential execution:

1. **Confirm vs. Cancel on DRAFT:** If `Cancel` processes first, the state becomes `CANCELLED` and subsequent `Confirm` is rejected. If `Confirm` processes first, state becomes `CONFIRMED` or `PENDING_APPROVAL`; subsequent `Cancel` may succeed under BR-04.
2. **Approval vs. Cancel on PENDING_APPROVAL:** If `Cancel` processes first, approval is rejected. If `Approve` processes first, state becomes `CONFIRMED`; subsequent `Cancel` may cancel the confirmed reservation before screening start time.
3. **Approval vs. Rejection:** Only the first valid decision leaves `PENDING_APPROVAL`. The second decision is rejected with `409 Conflict`.
4. **Approval vs. Expiration:** Approval is valid strictly before `approvalDeadline`. Once expiration is due ($t_{\text{current}} \ge \text{deadline}$), late approval cannot resurrect the hold.
5. **Stale Decision Protection:** A delayed decision on an old, released reservation cannot affect seats acquired by a newly created reservation.

---

### BR-07 — Approval Applicability & Authority

- Certain seats (e.g., VIP seats) require approval by an **Authorized Approver** before a reservation can become `CONFIRMED`.
- If a reservation contains a mix of standard and approval-required seats, the **entire reservation** enters `PENDING_APPROVAL` and holds all its seats atomically.
- Ownership of a reservation does not confer approval authority. Approver decisions require independent authorization.
- An approval decision applies to the entire reservation; partial approval of individual seats is prohibited.

---

### BR-08 — Pending Deadline, Release & Terminal Outcomes

- Every `PENDING_APPROVAL` reservation is assigned a finite `approvalDeadline` bounded by the screening start time:

$$\text{approvalDeadline} = \min(t_{\text{request}} + \text{timeout}, t_{\text{screening\_start}})$$

- Approval or rejection is allowed strictly before the deadline ($t_{\text{current}} < \text{approvalDeadline}$).
- At $t_{\text{current}} \ge \text{approvalDeadline}$, the hold expires automatically and transitions to `EXPIRED`, releasing all allocated resources.
- `REJECTED`, `EXPIRED`, and `CANCELLED` are **terminal states**. Terminal reservations cannot be reopened, retried, or returned to `DRAFT`.

---

### BR-09 — Observable Approval Progress & Notifications

- The reservation owner must be able to observe the current state, request time, `approvalDeadline`, decision status, and effective timestamps.
- **Notification Boundary:** Final confirmation notifications are dispatched only when a reservation enters `CONFIRMED` (via direct Confirm or Approve). Submitting a pending request does not send a final confirmation notification.
- Rejection, cancellation, and expiration initiate their respective outcome notifications asynchronously via the **Transactional Outbox Pattern**.

---

## 2. Invariant Mapping Matrix

| Rule ID | Name | Primary Owner / Component | Enforcement Mechanism |
| :--- | :--- | :--- | :--- |
| **BR-01** | Discrete Resource Scope | `Screening`, `Seat` | Composite Key `(screening_id, seat_id)` |
| **BR-02** | Exclusive Allocation | `Reservation Management`, `Persistence` | DB `UNIQUE` constraint on `active_allocation` / `PESSIMISTIC_WRITE` lock |
| **BR-03** | Creation Horizon | `ReservationService` | Pre-condition check ($t \le t_{\text{start}} - 15\text{m}$) |
| **BR-04** | Cancellation Policy | `ReservationService` | Owner verification + time check ($t < t_{\text{start}}$) |
| **BR-05** | Non-blocking Draft | `ReservationService` | `DRAFT` state bypasses conflict checks |
| **BR-06** | Lifecycle Concurrency | `Reservation Management` | Atomic transactions + Statechart guards |
| **BR-07** | Approval Authority | `ApprovalPolicy`, `ReservationService` | Role check (`X-User-Id` / Approver role) |
| **BR-08** | Finite Hold Expiration | `PendingExpirationPolicy`, `ExpirationService` | Pure evaluation ($t \ge \text{deadline}$) + pessimistic sweep |
| **BR-09** | Outcome Notifications | `Notification Integration` | Transactional Outbox post-commit events |