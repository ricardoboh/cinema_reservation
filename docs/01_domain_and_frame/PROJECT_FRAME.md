# Project Frame — Cinema Reservation System

**Team:** Deadlock  
**Project:** Cinema Seat Reservation System  
**Project Phase:** Baseline v0.1 / v0.2 Evolution (C01–C05)

---

## 1. Reservation Domain

The system provides and manages electronic seat reservations for specific movie screenings in a cinema.

A registered customer selects a specific movie screening via the web interface and chooses one or more available seats in the corresponding cinema hall. The selected seats then undergo the process of draft creation, confirmation (or approval within an approval workflow), or cancellation.

---

## 2. Purpose

The primary purpose of the system is to provide registered cinema customers with a reliable tool for seat selection and reservation, manage seat availability in real-time, strictly prevent double booking of the same resource, and provide both customers and operators with a transparent overview of reservation states and their lifecycle.

---

## 3. Users & Stakeholders

* **Registered User:**
  * A cinema customer logged into their user account.
  * *Privileges:* Creates new reservation drafts (`DRAFT`), checks seat availability, confirms their own reservations (`Confirm`), and cancels their own reservations (`Cancel`) prior to the screening start.
* **Authorized Approver:**
  * A dedicated role (e.g., cinema manager or box-office operator) introduced in specification v0.2.
  * *Privileges:* Reviews reservation requests requiring approval (e.g., premium/VIP seats) in the `PENDING_APPROVAL` state and performs `Approve` or `Reject` operations.
* **Cinema Administrator / Operator:**
  * Internal cinema management stakeholder.
  * *Privileges:* Manages the movie catalog, schedules movie screenings, defines cinema halls, and configures seating layouts and VIP zones.

---

## 4. Core Concepts

* **Resource:**
  * The fundamental unit of exclusivity in the system.
  * **Definition:** A unique pair `(Screening, Seat)` – a specific physical seat tied to a specific movie screening. A physical seat alone is not a standalone resource, as it can be reserved for different screenings at different times.
* **Reservation:**
  * The domain entity encapsulating the relationship between a customer, a screening, and the selected seats.
  * Holds the lifecycle state (`DRAFT`, `PENDING_APPROVAL`, `CONFIRMED`, `REJECTED`, `EXPIRED`, `CANCELLED`).
* **User:**
  * The identity of the registered customer who created and owns the reservation.
* **Screening:**
  * A scheduled movie showtime defined by a specific movie, cinema hall, and exact start time (`startTime` in UTC).
* **Seat:**
  * A physical seat in a cinema hall defined by hall reference, row number, and seat number. It may carry a classification (e.g., `STANDARD` vs. `VIP`).

---

## 5. Core Operations

1. **Create Reservation (`POST /reservations`):**
   * The customer selects a screening and seats. The system creates a reservation in the `DRAFT` state. This operation is non-blocking and does not verify allocation conflicts.
2. **Check Seat Availability (`GET /screenings/{id}/availability`):**
   * Queries seat statuses for a specific screening. Returns `AVAILABLE` or `UNAVAILABLE` (blocked by a confirmed allocation or an unexpired pending hold).
3. **Confirm Reservation (`POST /reservations/{id}/confirm`):**
   * The owner confirms a draft reservation. The system atomically verifies allocation conflicts:
     * If no seats require approval $\rightarrow$ transition to `CONFIRMED`.
     * If any seat requires approval $\rightarrow$ transition to `PENDING_APPROVAL` with a finite time limit (`approvalDeadline`).
4. **Cancel Reservation (`POST /reservations/{id}/cancel`):**
   * The owner cancels their reservation in `DRAFT`, `PENDING_APPROVAL`, or `CONFIRMED` state prior to the screening start. Transitions to `CANCELLED` and releases holds/allocations.
5. **Approve / Reject Reservation (`POST /reservations/{id}/approval`):**
   * An Authorized Approver decides on a reservation in the `PENDING_APPROVAL` state. Results in `CONFIRMED` or `REJECTED`.

---

## 6. Persistent State

### Reservation Entity
* Reservation ID (`reservationId`)
* Registered User ID (`userId`)
* Screening ID (`screeningId`)
* Selected seat references (`seatIds`)
* Reservation status (`status`: `DRAFT`, `PENDING_APPROVAL`, `CONFIRMED`, `REJECTED`, `EXPIRED`, `CANCELLED`)
* Creation timestamp (`createdAt`)
* Approval time limit (`approvalDeadline` – optional)
* Effective expiration/decision timestamp (`expiredAt`, `decisionAt` – optional)

### Resource & Seat Entity
* Hall ID (`hallId`)
* Row number (`rowNumber`) and seat number (`seatNumber`)
* Seat type (`STANDARD`, `VIP`)
* Active allocation (`active_allocation` – relational mapping of `screening_id` + `seat_id` + `reservation_id`)

### Screening Entity
* Screening ID (`screeningId`)
* Movie ID (`movieId`)
* Hall ID (`hallId`)
* Screening start time (`startsAt` / `startTime` in UTC)

---

## 7. State Model Evolution (State-Changing Operations)

The reservation lifecycle evolved from Baseline v0.1 to Baseline v0.2:

* **Baseline v0.1:**
  * `DRAFT → CONFIRMED` (Direct confirmation)
  * `DRAFT → CANCELLED` (Draft cancellation)
  * `CONFIRMED → CANCELLED` (Confirmed reservation cancellation)

* **Baseline v0.2 (Approval Workflow Extension):**
  * `DRAFT → PENDING_APPROVAL` (Confirmation of a reservation containing approval-required/VIP seats)
  * `PENDING_APPROVAL → CONFIRMED` (Approval by an authorized role: `APPROVE`)
  * `PENDING_APPROVAL → REJECTED` (Rejection by an authorized role: `REJECT`)
  * `PENDING_APPROVAL → EXPIRED` (Automatic expiration upon reaching `approvalDeadline`)
  * `PENDING_APPROVAL → CANCELLED` (Pending reservation cancellation initiated by owner)

---

## 8. Summary of Core Domain Invariants

*(Detailed specifications of permissions and boundaries are isolated in `DOMAIN_RULES.md`)*

* **Exclusive Resource Invariant (BR-02):** No two valid reservations in `CONFIRMED` or `PENDING_APPROVAL` states may contain the same `Resource` `(Screening, Seat)` at any point in time.
* **Domain Creation Horizon (BR-03):** A reservation can be created at most 15 minutes before the scheduled screening start time ($t \le t_{start} - 15\text{ min}$).
* **Finite Hold Validity (BR-08):** No reservation in `PENDING_APPROVAL` state may hold seats indefinitely; it expires no later than the screening start time.

---

## 9. External Boundaries & Dependencies

* **Notification Service:**
  * The system uses an external notification interface to inform registered users about final reservation state transitions (`CONFIRMED`, `CANCELLED`, `REJECTED`, `EXPIRED`).
  * *Architectural Pattern:* Designed via the **Transactional Outbox Pattern** – outbox events are persisted in the database within the same transaction as the reservation update and dispatched asynchronously post-commit.

---

## 10. Assumptions & Unknowns

* **Assumption:**
  * Each movie screening has a pre-assigned cinema hall and seating layout. Once reservation sales open for a screening, the hall structure and seating layout do not change.
* **Identified Unknown / TBD:**
  * The final production TTL/timeout for automatically expiring abandoned `DRAFT` reservations (in baseline v0.2, expiration is implemented for `PENDING_APPROVAL`, but unconfirmed `DRAFT` reservations have no explicit TTL assigned yet).