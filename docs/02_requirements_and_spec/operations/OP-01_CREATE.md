# OP-01 — Create Reservation Specification

**Domain Operation:** Create Reservation Proposal (`DRAFT`)  
**Primary Actor:** Registered User  
**Target Resource:** `(Screening, Seat)`  
**Baseline Version:** v0.1 / v0.2 Consistent  

---

## 1. Goal & Business Value

A registered cinema customer creates a non-binding reservation proposal (`DRAFT`) for one or more seats for a specific movie screening. 

Creating a draft captures the customer's intent without immediately locking or acquiring the resources, allowing users to assemble multi-seat selections or review booking details before committing to exclusive allocation.

---

## 2. Trigger & Endpoint Mapping

* **Triggering Event:** User selects screening and desired physical seats in the web interface and clicks "Create Draft".
* **HTTP Method:** `POST`
* **API Endpoint:** `/reservations`
* **Request Payload:**
  ```json
  {
    "userId": 1,
    "screeningId": 42,
    "seatIds": [10, 11]
  }

```

---

## 3. Observable Requirements & Invariants

* **REQ-01 (Unchanged):** The system shall create exactly one new reservation in `DRAFT` state if the specified user, screening, and seats exist, all seats belong to the screening's hall layout, and creation horizon BR-03 is satisfied.
* **BR-01 (Scope):** Availability and allocations are evaluated per `(Screening, Seat)`.
* **BR-03 (Creation Horizon):** Creation is permitted strictly when $t_{\text{current}} \le t_{\text{screening\_start}} - 15\text{ minutes}$.
* **BR-05 (Non-blocking Draft):** `DRAFT` creation performs **no collision check** against existing reservations (including `CONFIRMED` or `PENDING_APPROVAL` holds). Multiple users may hold overlapping drafts simultaneously.

---

## 4. Execution Flow (Main Success Scenario)

1. **Submit Request:** User submits `userId`, `screeningId`, and a non-empty array of `seatIds`.
2. **Validate References:** System verifies that the specified user and screening exist in the database.
3. **Validate Hall Membership:** System verifies that all requested `seatIds` exist and belong to the hall assigned to the screening.
4. **Validate Horizon (BR-03):** System checks that $t_{\text{current}} \le t_{\text{screening\_start}} - 15\text{ minutes}$.
5. **Persist Draft:** System generates a unique `reservationId` and persists a new `Reservation` entity with status `DRAFT` and creation timestamp.
6. **Return Response:** System returns `HTTP 201 Created` with the generated reservation payload.

---

## 5. Alternative & Error Outcomes

| Condition | Response Code | Error Details | State Effect |
| --- | --- | --- | --- |
| **Missing User / Screening / Seat** | `404 Not Found` | Entity reference does not exist | No reservation created |
| **Empty or Invalid Seat List** | `400 Bad Request` | `seatIds` is empty or invalid | No reservation created |
| **Seat Outside Screening Hall** | `400 Bad Request` | Seat belongs to another hall layout | No reservation created |
| **Creation Horizon Violation** | `409 Conflict` | $t_{\text{current}} > t_{\text{screening\_start}} - 15\text{m}$ | No reservation created |

---

## 6. Verification Scenarios

* **Scenario V-01.1 (Standard Success):** Screening starts in 2 hours. User selects valid seats A1, A2 $\rightarrow$ `201 Created`, state `DRAFT`.
* **Scenario V-01.2 (Exact Boundary):** Screening starts in exactly 15 minutes $\rightarrow$ `201 Created`, state `DRAFT`.
* **Scenario V-01.3 (Late Creation):** Screening starts in 14 minutes and 59 seconds $\rightarrow$ `409 Conflict`, draft creation rejected.
* **Scenario V-01.4 (Overlapping Drafts):** User 1 creates draft for Seat A1. User 2 creates draft for Seat A1 $\rightarrow$ Both requests return `201 Created` (`DRAFT` does not block).
