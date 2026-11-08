```markdown
# OP-05 — Approve / Reject Reservation Specification

**Domain Operation:** Approve or Reject Pending Reservation  
**Primary Actor:** Authorized Approver (Cinema Manager / Staff)  
**Target Resource:** `(Screening, Seat)`  
**Baseline Version:** v0.2 New Operation  

---

## 1. Goal & Business Value

An Authorized Approver makes an independent managerial decision on a reservation holding seats in the `PENDING_APPROVAL` state.

This operation resolves delayed approval requests, converting temporary holds into final confirmed allocations (`APPROVE`) or releasing seats back to the public pool upon refusal (`REJECT`).

---

## 2. Trigger & Endpoint Mapping

* **Triggering Event:** Authorized Approver reviews pending requests and clicks "Approve" or "Reject".
* **HTTP Method:** `POST`
* **API Endpoint:** `/reservations/{id}/approval`
* **Request Header:** `X-User-Id: <approverId>`
* **Request Payload:**
  ```json
  {
    "decision": "APPROVE"
  }

```

*(OR `"decision": "REJECT"`)*

---

## 3. Observable Requirements & Invariants

* **REQ-08 (Authority Guard):** Only an Authorized Approver may execute this operation. Unauthorized attempts (including by the reservation owner) receive `403 Forbidden`.
* **REQ-09 (Positive Approval):** `decision = APPROVE` atomically updates state from `PENDING_APPROVAL` to `CONFIRMED`, maintains seat allocations without release windows, and dispatches a final confirmation notification.
* **REQ-10 (Negative Rejection):** `decision = REJECT` atomically updates state from `PENDING_APPROVAL` to `REJECTED`, releases all seat holds, and dispatches a rejection notification.
* **BR-06 / BR-08 (Time & Stale Protection):** Decision requires $t_{\text{current}} < \text{approvalDeadline}$ and $t_{\text{current}} < t_{\text{screening\_start}}$. Deciding on an expired or cancelled reservation returns `409 Conflict`.

---

## 4. Execution Flow (Main Success Scenario)

1. **Authenticate & Lock:** System verifies caller identity (`X-User-Id`) has Authorized Approver privileges and acquires screening lock.
2. **Validate State & Deadline:** System verifies reservation exists, state is `PENDING_APPROVAL`, $t_{\text{current}} < \text{approvalDeadline}$, and $t_{\text{current}} < t_{\text{screening\_start}}$.
3. **Execute Decision:**
* **If APPROVE:** State transitions to `CONFIRMED`. Active seat allocations maintained. Outbox event created for final confirmation notification.
* **If REJECT:** State transitions to `REJECTED`. Active seat holds deleted. Outbox event created for rejection notification.


4. **Record Metadata:** System records `decisionAt` timestamp and `approverId`.
5. **Commit & Return:** Transaction commits, releasing locks. System returns `HTTP 200 OK` with updated reservation payload.

---

## 5. Alternative & Error Outcomes

| Condition | Response Code | Error Details | State Effect |
| --- | --- | --- | --- |
| **Missing Reservation** | `404 Not Found` | `reservationId` does not exist | State unchanged |
| **Unauthorized Caller** | `403 Forbidden` | Caller lacks Approver authority | State unchanged |
| **Invalid Decision Value** | `400 Bad Request` | Payload decision is not `APPROVE` or `REJECT` | State unchanged |
| **Non-Pending State** | `409 Conflict` | Reservation is in `DRAFT`, `CONFIRMED`, `CANCELLED`, etc. | State unchanged |
| **Deadline Elapsed** | `409 Conflict` | $t_{\text{current}} \ge \text{approvalDeadline}$ (Resolved as `EXPIRED`) | Reconciled as `EXPIRED` |

---

## 6. Verification Scenarios

* **Scenario V-05.1 (Authorized Approval):** Approver 3 submits `APPROVE` for live pending reservation $\rightarrow$ `200 OK`, status `CONFIRMED`, seats remain locked.
* **Scenario V-05.2 (Authorized Rejection):** Approver 3 submits `REJECT` for live pending reservation $\rightarrow$ `200 OK`, status `REJECTED`, seats immediately released.
* **Scenario V-05.3 (Unauthorized Decision):** Reservation owner (User 1) attempts to approve their own pending reservation $\rightarrow$ `403 Forbidden`, state remains `PENDING_APPROVAL`.
* **Scenario V-05.4 (Late Approval):** `approvalDeadline` passed 10 seconds ago. Approver submits `APPROVE` $\rightarrow$ `409 Conflict`, reservation reconciled to `EXPIRED`, seats released.
* **Scenario V-05.5 (Race Condition):** Approver submits `APPROVE` simultaneously as owner submits `CANCEL` $\rightarrow$ Exactly one operation succeeds; second gets `409 Conflict`.

```