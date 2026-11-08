```markdown
# OP-03 — Confirm Reservation Specification

**Domain Operation:** Confirm Reservation & Acquire Allocation  
**Primary Actor:** Reservation Owner (Registered User)  
**Target Resource:** `(Screening, Seat)`  
**Baseline Version:** v0.2 Conditional Approval Branching  

---

## 1. Goal & Business Value

The owner of a `DRAFT` reservation submits it for binding resource allocation.

Confirm acts as the exclusive allocation boundary: it converts non-blocking drafts into either immediate final bookings (`CONFIRMED`) or temporary exclusive holds awaiting managerial review (`PENDING_APPROVAL`).

---

## 2. Trigger & Endpoint Mapping

* **Triggering Event:** Reservation owner clicks "Confirm Booking".
* **HTTP Method:** `POST`
* **API Endpoint:** `/reservations/{id}/confirm`
* **Request Header:** `X-User-Id: <userId>`
* **Response Payload (Pending Approval Branch):**
  ```json
  {
    "reservationId": 101,
    "status": "PENDING_APPROVAL",
    "approvalDeadline": "2026-10-24T18:00:00Z",
    "seats": [10, 11]
  }

```

---

## 3. Observable Requirements & Invariants

* **REQ-03 (Updated v0.2):** A valid draft with no competing allocations becomes `CONFIRMED` if no seat requires approval, or `PENDING_APPROVAL` if at least one seat requires approval.
* **REQ-04 (Concurrency):** During concurrent confirmation attempts for overlapping reservations, at most one reservation can acquire the active allocation. Competing attempts fail with `409 Conflict`.
* **BR-02 (Exclusivity):** Allocation acquisition is **atomic** across all seats in the reservation. No partial seat acquisition is allowed.
* **BR-07 (Approval Policy):** If any seat in a reservation is classified as VIP / approval-required, the whole reservation enters `PENDING_APPROVAL`.
* **BR-08 (Deadline Assignment):** Pending reservations receive a finite `approvalDeadline` $=\min(t_{\text{request}} + \text{timeout}, t_{\text{screening\_start}})$.

---

## 4. Execution Flow (Main Success Scenario)

1. **Authenticate & Lock:** System verifies caller identity (`X-User-Id`) and acquires a pessimistic write lock for the screening.
2. **Validate State & Time:** System verifies reservation exists, caller is owner, state is `DRAFT`, and $t_{\text{current}} < t_{\text{screening\_start}}$.
3. **Evaluate Approval Policy:** System evaluates seat types across all selected seats.
4. **Atomic Conflict Check:** System verifies that no seat in the reservation is currently allocated to another `CONFIRMED` or unexpired `PENDING_APPROVAL` reservation.
5. **Branch Execution:**
* **Branch A (No Approval Required):** State updated to `CONFIRMED`. Outbox event created for final confirmation notification.
* **Branch B (Approval Required):** State updated to `PENDING_APPROVAL`, `approvalDeadline` computed and stored. Holds placed on all seats.


6. **Commit & Return:** Transaction commits, releasing locks. System returns `HTTP 200 OK` with updated status.

---

## 5. Alternative & Error Outcomes

| Condition | Response Code | Error Details | State Effect |
| --- | --- | --- | --- |
| **Missing Reservation** | `404 Not Found` | `reservationId` does not exist | State unchanged |
| **Caller Not Owner** | `403 Forbidden` | `X-User-Id` does not match reservation owner | State unchanged |
| **Invalid State** | `409 Conflict` | Reservation is not in `DRAFT` state | State unchanged |
| **Screening Already Started** | `409 Conflict` | $t_{\text{current}} \ge t_{\text{screening\_start}}$ | State unchanged |
| **Allocation Conflict** | `409 Conflict` | At least one seat is held by another reservation | Reservation remains `DRAFT` (no partial hold) |

---

## 6. Verification Scenarios

* **Scenario V-03.1 (Standard Confirmation):** Draft contains standard seats A1, A2. No conflicts $\rightarrow$ `200 OK`, status `CONFIRMED`.
* **Scenario V-03.2 (Conditional Pending Hold):** Draft contains standard A1 and VIP A2 $\rightarrow$ `200 OK`, status `PENDING_APPROVAL`, `approvalDeadline` set, both seats unavailable.
* **Scenario V-03.3 (Conflicting Confirmation):** Draft R1 (User 1) and Draft R2 (User 2) contain Seat A1. R1 confirms first $\rightarrow$ R1 gets `CONFIRMED`; R2 confirm attempt gets `409 Conflict`, R2 stays `DRAFT`.
* **Scenario V-03.4 (Multi-Seat Partial Conflict):** Draft R1 contains A1, A2. A1 is already confirmed $\rightarrow$ `409 Conflict`, neither A1 nor A2 is allocated to R1.

```