# Change Impact Analysis — Baseline v0.1 to v0.2 Evolution

**Team:** Deadlock  
**Domain:** Cinema Seat Reservation System  
**Baseline Target:** Baseline v0.2 Approval Workflow  
**Document Purpose:** Pre-specification Analysis of Changes and Architectural Impact

---

## 1. Proposed Change Definition

Certain high-value or classified resources (e.g., VIP seats) require formal authorization by an **Authorized Approver** before a reservation can transition to `CONFIRMED`. 

Approval requests may experience processing delays, negative decisions (rejection), or automatic expiration due to elapsed timeouts. This document evaluates the systematic impact of incorporating this approval workflow into Specification Baseline v0.1.

---

## 2. Granular Impact Analysis Across Baseline Operations

### Create Reservation (OP-01)
- **Impact Level:** None (No structural change required).
- **Analysis:** Create continues to initialize reservations strictly in the `DRAFT` state. Under BR-05, a `DRAFT` represents non-binding user intent and does not create any resource allocation or invoke approval applicability checks.

### Check Availability (OP-02)
- **Impact Level:** High (Rule & Query Update).
- **Analysis:** `PENDING_APPROVAL` must temporarily block resource availability.
- **Rationale:** If a seat in a pending reservation remained `AVAILABLE`, a competing customer could confirm a standard reservation for the same seat before the pending request was reviewed, causing double booking. The block must be released if the pending request is rejected, cancelled, or expired.

### Confirm Reservation (OP-03)
- **Impact Level:** High (Branching Logic & State Transitions).
- **Analysis:** Confirm is no longer a mandatory direct transition from `DRAFT` to `CONFIRMED`.
  - **Standard Resources:** $\text{DRAFT} \rightarrow \text{CONFIRMED}$ (Immediate final allocation).
  - **Approval-Required Resources:** $\text{DRAFT} \rightarrow \text{PENDING\_APPROVAL}$ (Temporary hold + finite deadline assignment).

### Approve / Reject Reservation (OP-05)
- **Impact Level:** High (New System Operation & Actor).
- **Analysis:** Introduces a new primary actor (**Authorized Approver**) and an independent operational goal.
  - $\text{PENDING\_APPROVAL} \rightarrow \text{CONFIRMED}$ (`APPROVE`)
  - $\text{PENDING\_APPROVAL} \rightarrow \text{REJECTED}$ (`REJECT`)
- Ownership alone does not confer decision privileges; explicit authority validation is required.

### Cancel Reservation (OP-04)
- **Impact Level:** Medium (State Eligibility Extension).
- **Analysis:** Owner cancellation must be extended to permit withdrawal from `PENDING_APPROVAL`:
  - $\text{PENDING\_APPROVAL} \rightarrow \text{CANCELLED}$
- Cancelling a pending reservation immediately releases all associated temporary resource holds.

---

## 3. Extended Reservation State Lifecycle

The v0.1 three-state model (`DRAFT`, `CONFIRMED`, `CANCELLED`) is insufficient to represent delayed decisions and terminal outcomes.

### Expanded Lifecycle Transitions
```text
DRAFT            --> CONFIRMED          (Direct Confirm - No approval required)
DRAFT            --> PENDING_APPROVAL   (Conditional Confirm - Approval required)
PENDING_APPROVAL --> CONFIRMED          (Authorized APPROVE decision)
PENDING_APPROVAL --> REJECTED           (Authorized REJECT decision)
PENDING_APPROVAL --> EXPIRED            (System deadline elapsed)
PENDING_APPROVAL --> CANCELLED           (Owner cancellation prior to screening)

```

* `REJECTED`, `EXPIRED`, and `CANCELLED` are defined as **terminal states**. Terminal records are retained for auditing/history and cannot be reopened.

---

## 4. Verification & Testing Requirements

The introduced change requires new verification scenarios to ensure system stability and rule enforcement:

1. **Pending Hold Acquisition:** Approval-required resources enter `PENDING_APPROVAL` upon Confirm and block availability.
2. **Delayed Decision Observation:** Pending hold remains observable and exclusive while awaiting decision.
3. **Successful Approval:** Positive decision converts hold to `CONFIRMED` without a release window.
4. **Authorized Rejection:** Negative decision transitions to `REJECTED` and releases resources.
5. **Automatic Expiration:** Reaching `approvalDeadline` transitions to `EXPIRED` and releases resources.
6. **Pending Cancellation:** Owner cancels pending hold prior to screening start.
7. **Resource Availability Recovery:** Seats become `AVAILABLE` immediately following rejection, expiration, or cancellation (unless re-allocated).
8. **Concurrency & Race Conditions:** Concurrent handling of decision vs. cancellation, decision vs. expiration, and stale decision rejection.

---

## 5. Architectural Drivers Exposed for C03

The change impact analysis identifies key architectural requirements carried forward into system design (C03):

* **Persistent Delayed Workflow:** Approval state, policy snapshot, and deadline must survive process restarts.
* **Role & Authority Boundaries:** Separation of reservation ownership from approval authority.
* **Authoritative Time & Expiration:** Reliable finite deadline enforcement and scheduled background sweep processing.
* **Atomic Multi-seat Hold Management:** Atomic allocation conversion/release across multi-seat reservations.
* **Lifecycle-Aligned Notifications:** Asynchronous dispatch of outcome notifications via the Transactional Outbox Pattern.

---

## 6. Conclusion & Acceptance Record

The proposed change impacts Confirm, Availability, Cancel, the domain state model, use cases, verification suites, and architectural drivers.

Create remains conceptually unchanged. Specification Baseline v0.1 remains frozen as a historical milestone, while Specification Baseline v0.2 incorporates the full scope of these accepted analysis outcomes.
