```markdown
# OP-04 — Cancel Reservation Specification

**Domain Operation:** Cancel Reservation & Release Allocation  
**Primary Actor:** Reservation Owner (Registered User)  
**Target Resource:** `(Screening, Seat)`  
**Baseline Version:** v0.2 Multi-State Cancellation  

---

## 1. Goal & Business Value

The owner of a reservation cancels an unwanted draft, pending hold, or confirmed reservation prior to the screening start.

Cancellation allows customers to relinquish reservations they no longer intend to use, immediately releasing allocated or held seats back into the available pool while retaining historical records for auditing.

---

## 2. Trigger & Endpoint Mapping

* **Triggering Event:** Reservation owner clicks "Cancel Reservation".
* **HTTP Method:** `POST`
* **API Endpoint:** `/reservations/{id}/cancel`
* **Request Header:** `X-User-Id: <userId>`
* **Response Payload:**
  ```json
  {
    "reservationId": 101,
    "status": "CANCELLED",
    "cancelledAt": "2026-10-24T17:30:00Z"
  }

```

---

## 3. Observable Requirements & Invariants

* **REQ-05 (Updated v0.2):** The owner may cancel a reservation in `DRAFT`, `PENDING_APPROVAL`, or `CONFIRMED` state if the screening has not started.
* **REQ-06 (Unchanged):** Successful cancellation updates state to `CANCELLED`, retains historical records, and stops blocking resource availability.
* **BR-04 (Cancellation Policy):** Cancellation requires $t_{\text{current}} < t_{\text{screening\_start}}$. Repeated cancellation or cancellation from terminal states (`CANCELLED`, `REJECTED`, `EXPIRED`) returns `409 Conflict`.
* **BR-06 (Concurrency):** Concurrent `Confirm`/`Cancel` or `Approve`/`Cancel` operations produce valid sequential outcomes without state corruption or leaked allocations.

---

## 4. Execution Flow (Main Success Scenario)

1. **Authenticate & Lock:** System verifies caller identity (`X-User-Id`) and acquires screening lock.
2. **Validate Ownership & State:** System verifies reservation exists, caller is owner, state $\in \{\text{DRAFT}, \text{PENDING\_APPROVAL}, \text{CONFIRMED}\}$, and $t_{\text{current}} < t_{\text{screening\_start}}$.
3. **Update State & Release:** System updates state to `CANCELLED`, records `cancelledAt` timestamp, and removes active seat allocations/holds.
4. **Dispatch Outbox Event:** System creates a cancellation outbox event for notification processing.
5. **Commit & Return:** Transaction commits, releasing locks. System returns `HTTP 200 OK` with updated status.

---

## 5. Alternative & Error Outcomes

| Condition | Response Code | Error Details | State Effect |
| --- | --- | --- | --- |
| **Missing Reservation** | `404 Not Found` | `reservationId` does not exist | State unchanged |
| **Caller Not Owner** | `403 Forbidden` | `X-User-Id` does not match owner | State unchanged |
| **Terminal / Invalid State** | `409 Conflict` | State is `CANCELLED`, `REJECTED`, or `EXPIRED` | State unchanged |
| **Screening Already Started** | `409 Conflict` | $t_{\text{current}} \ge t_{\text{screening\_start}}$ | State unchanged |

---

## 6. Verification Scenarios

* **Scenario V-04.1 (Cancel Draft):** Owner cancels `DRAFT` reservation before screening $\rightarrow$ `200 OK`, status `CANCELLED`.
* **Scenario V-04.2 (Cancel Pending Hold):** Owner cancels `PENDING_APPROVAL` reservation $\rightarrow$ `200 OK`, status `CANCELLED`, held seats immediately become `AVAILABLE`.
* **Scenario V-04.3 (Cancel Confirmed):** Owner cancels `CONFIRMED` reservation $\rightarrow$ `200 OK`, status `CANCELLED`, allocated seats immediately become `AVAILABLE`.
* **Scenario V-04.4 (Late Cancel Attempt):** Screening started 1 minute ago. Owner attempts cancel $\rightarrow$ `409 Conflict`, state remains unchanged.
* **Scenario V-04.5 (Repeated Cancel):** Owner attempts to cancel an already `CANCELLED` reservation $\rightarrow$ `409 Conflict`.

```