# C02 Task 14 Implementation and Verification Report

**Team:** Deadlock  
**Domain:** Cinema Seat Reservation System  
**Baseline Stage:** Runnable Baseline v0.2 (Task 14 Completion)  
**Verification Date:** 2026-10-04  

---

## 1. Scope & Objective

This report provides engineering evidence for the completion of **Task 14 (Runnable Baseline v0.2)**. 

The primary objective was to expand the original Baseline v0.1 application (`DRAFT`, `CONFIRMED`, `CANCELLED`) to fully implement the conditional Approval Workflow specified in Baseline v0.2 (`PENDING_APPROVAL`, `REJECTED`, `EXPIRED`, and owner/approver authority checks) without breaking existing functionality.

---

## 2. File Inventory (Changes Executed)

All paths are relative to the repository root. Package prefix: `backend/src/main/java/com/reservedbytes/cinema_reservation/`.

### Newly Created Files
* **`dto/ApprovalRequest.java`:** Explicit payload structure for `APPROVE` / `REJECT` decisions.
* **`service/ApprovalPolicy.java`:** Deterministic policy evaluating approval-required seats (VIP classification).
* **`service/ExpirationService.java`:** Transactional execution of expiration transitions under pessimistic locks.
* **`service/ExpirationSweep.java`:** Scheduled background worker resolving due pending holds.
* **`frontend/verify-ui.mjs`:** CDP automation script for headless real-browser testing.

### Modified Backend & Frontend Files
* **`model/ReservationStatus.java`:** Added `PENDING_APPROVAL`, `REJECTED`, and `EXPIRED` lifecycle states.
* **`model/Reservation.java`:** Persisted pending policy snapshots, `approvalDeadline`, and decision timestamps.
* **`repository/ReservationRepository.java`:** Added due-aware observation queries and pending hold exclusion queries.
* **`service/ReservationService.java`:** Implemented conditional confirmation, authority validation, and terminal state guards.
* **`controller/ReservationController.java`:** Added status read GET surface and approval POST endpoint.
* **`frontend/src/App.tsx` & `reservations.ts`:** Responsive UI layout displaying lifecycle badges and independent Approver controls.

---

## 3. Backend Operational Semantics

| Operation | v0.2 Implemented Outcome |
| :--- | :--- |
| **Create** | `201 Created`; persists non-blocking `DRAFT` (same 15-minute horizon validation). |
| **Availability** | `200 OK`; returns `UNAVAILABLE` for `CONFIRMED` and unexpired `PENDING_APPROVAL` holds. |
| **Confirm** | Evaluates seat policy; standard seats become `CONFIRMED`, VIP seats become `PENDING_APPROVAL`. Conflicting allocations return `409 Conflict`. |
| **Approve (OP-05)** | Authorized Approver transitions live pending hold to `CONFIRMED`. Non-approver attempts return `403 Forbidden`. |
| **Reject (OP-05)** | Authorized Approver transitions live pending hold to `REJECTED` and releases held seats. |
| **Cancel (OP-04)** | Owner cancels `DRAFT`, `PENDING_APPROVAL`, or `CONFIRMED` prior to screening start, releasing seats. |
| **Expire (System)** | Reaching `approvalDeadline` automatically transitions pending hold to `EXPIRED` and releases seats. |

---

## 4. Automated Verification Results

* **Backend Test Suite (`mvnw clean package`):**
  * **Result:** `BUILD SUCCESS`
  * **Executions:** 60 backend tests passed (0 failures, 0 errors, 0 skipped).
  * **Coverage:** Includes unit/integration tests for mixed allocations, 403 authority checks, 409 pending conflicts, nanosecond-level boundary checks (59s/60s/61s), and race conditions.
* **Frontend Verification (`npm run lint` & `npm run build`):**
  * **Result:** PASS (TypeScript compilation clean, Vite bundle generated).

---

## 5. Live Runtime & End-to-End API Verification

Verification executed via `verify-baseline.ps1` against running dev profile (43 automated HTTP assertions):

| Flow ID | Scenario Description | Observable Result |
| :---: | :--- | :--- |
| **Flow A** | Ordinary non-approval confirmation & cancellation | `DRAFT` $\rightarrow$ `CONFIRMED` $\rightarrow$ `CANCELLED` (Seats released) |
| **Flow B** | Mixed reservation hold & authorized approval | Entering `PENDING_APPROVAL` holds all seats; owner `APPROVE` returns `403`; Approver 3 `APPROVE` returns `CONFIRMED`. |
| **Flow C** | Rejection workflow | `PENDING_APPROVAL` $\rightarrow$ `REJECTED`; seats immediately returned to `AVAILABLE`. |
| **Flow D** | Pending cancellation workflow | Owner cancels pending hold $\rightarrow$ `CANCELLED`; seats released. |
| **Flow E** | Finite 60-second deadline expiration | `approvalDeadline` reached $\rightarrow$ `EXPIRED`; late approval returns `409 Conflict`. |
| **Flow F** | Concurrent/competing pending confirmation | Competing draft confirmation against active hold returns `409 Conflict`. |

---

## 6. Real-Browser Headless Edge Verification

Executed via CDP script (`verify-ui.mjs`) on an unmocked running application:
* **UI Rendering:** Verified 390px mobile viewport (no horizontal scrollbar overflow).
* **Badge Dynamics:** Real-time polling updated lifecycle badges correctly (`PENDING_APPROVAL`, `CONFIRMED`, `EXPIRED`).
* **Authority UI:** Non-authorized approval buttons visibly returned `403 Forbidden` notifications in the interface.

---

## 7. Conclusion

Baseline v0.2 is fully runnable, tested, and verified. The code maintains absolute backward compatibility with Baseline v0.1 while enforcing all newly introduced invariants (BR-07, BR-08, BR-09).