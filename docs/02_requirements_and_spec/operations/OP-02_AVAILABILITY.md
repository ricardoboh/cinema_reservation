```markdown
# OP-02 — Check Availability Specification

**Domain Operation:** Check Seat Availability  
**Primary Actor:** Registered User / Anonymous Visitor  
**Target Resource:** `(Screening, Seat)`  
**Baseline Version:** v0.2 Enhanced  

---

## 1. Goal & Business Value

A user inspects the real-time availability of physical seats for a specific movie screening.

This operation provides an accurate, read-only projection of available versus unavailable seats, ensuring users do not select occupied or temporarily held seats during layout selection.

---

## 2. Trigger & Endpoint Mapping

* **Triggering Event:** User navigates to the seating map view for a specific screening.
* **HTTP Method:** `GET`
* **API Endpoint:** `/screenings/{screeningId}/availability`
* **Response Payload:**
  ```json
  [
    { "seatId": 10, "rowNumber": 1, "seatNumber": 1, "seatType": "STANDARD", "status": "AVAILABLE" },
    { "seatId": 11, "rowNumber": 1, "seatNumber": 2, "seatType": "VIP", "status": "UNAVAILABLE" }
  ]

```

---

## 3. Observable Requirements & Invariants

* **REQ-02 (Updated v0.2):** For an existing screening, the system marks a seat as `UNAVAILABLE` if its resource belongs to a reservation in `CONFIRMED` state OR an unexpired `PENDING_APPROVAL` state. Otherwise, it is marked `AVAILABLE`.
* **BR-02 (Exclusivity):** Active allocations cover both confirmed reservations and live pending holds.
* **BR-08 (Due Expiration):** If a pending reservation's `approvalDeadline` has elapsed ($t_{\text{current}} \ge \text{deadline}$), its seats must be reflected as `AVAILABLE` (due expiration reconciliation).
* **Read-Only Invariant:** Executing `Check Availability` alters no persistent domain state or reservation records.

---

## 4. Execution Flow (Main Success Scenario)

1. **Submit Request:** Client requests seat availability for `screeningId`.
2. **Validate Screening:** System verifies that the screening exists.
3. **Reconcile Expired Holds:** System identifies any pending reservations for the screening where $t_{\text{current}} \ge \text{approvalDeadline}$ and reconciles their status to `EXPIRED`.
4. **Fetch Hall Layout:** System retrieves all physical seats belonging to the screening's hall.
5. **Project Availability:** For each seat, system queries active allocations (`CONFIRMED` or live `PENDING_APPROVAL`).
6. **Return Projection:** System maps seats to `AVAILABLE` or `UNAVAILABLE` statuses and returns `HTTP 200 OK`.

---

## 5. Alternative & Error Outcomes

| Condition | Response Code | Error Details | State Effect |
| --- | --- | --- | --- |
| **Missing Screening** | `404 Not Found` | `screeningId` does not exist | None |

---

## 6. Verification Scenarios

* **Scenario V-02.1 (Free Hall):** Screening has no confirmed or pending reservations $\rightarrow$ All seats returned as `AVAILABLE`.
* **Scenario V-02.2 (Confirmed Block):** Seat A1 belongs to a `CONFIRMED` reservation $\rightarrow$ A1 returned as `UNAVAILABLE`.
* **Scenario V-02.3 (Pending Hold Block):** Seat A2 belongs to an unexpired `PENDING_APPROVAL` reservation $\rightarrow$ A2 returned as `UNAVAILABLE`.
* **Scenario V-02.4 (Draft Non-Block):** Seat A3 belongs only to one or more `DRAFT` reservations $\rightarrow$ A3 returned as `AVAILABLE`.
* **Scenario V-02.5 (Post-Expiration Release):** Seat A4 was in `PENDING_APPROVAL`, but `approvalDeadline` has passed $\rightarrow$ Reconciled as `EXPIRED`, A4 returned as `AVAILABLE`.

```