# DD-Derived Verification Suite & Test Matrix — Manage Pending Expiration

**Team:** Deadlock

**Domain:** Cinema Seat Reservation System

**Selected Architecture Unit:** Manage Pending Expiration (`Reservation Management`, C03 C2/G2)

**Baseline Alignment:** Baseline v0.2 / C05 Detailed Design

**Target File:** `docs/05_verification_and_quality/DD_DERIVED_TESTS.md`

---

## 1. Overview & Verification Strategy

The verification suite for the **Manage Pending Expiration** unit is strictly derived from the C05 Detailed Design specification. It follows a two-tier testing strategy designed to balance isolated unit isolation with transactional integration guarantees:

* **Level 1 (Unit Tests):** Fast, in-memory execution targeting pure mathematical formulas, boundary conditions (nanosecond precision), fast-fail preconditions, and state isolation without Spring framework or database dependencies.
* **Level 2 (Component / Integration Tests):** Spring Boot context and embedded H2 database execution testing coarse pessimistic locking, atomic multi-seat hold releases, transaction boundaries, rollback behavior, and post-commit transactional event suppression.

---

## 2. Test Mapping Matrix (Detailed Design $\rightarrow$ Test Code)

| DD Element / Decision ID | Domain Invariant / Design Rule | Test Level | Test Suite & Method Name | Expected Behavior & Verification Criteria |
| --- | --- | --- | --- | --- |
| **D2** | Timeout projection when screening is far in the future | Unit | `PendingExpirationPolicyTests.usesTimeoutWhenScreeningIsLater` | $t_{\text{deadline}} = t_{\text{requested}} + \text{timeout}$. |
| **D2** | Capped deadline at screening start time | Unit | `PendingExpirationPolicyTests.capsDeadlineAtScreeningIncludingEquality` | $t_{\text{deadline}} = t_{\text{screening\_start}}$ when request time plus timeout exceeds start time. |
| **D2** | Duration overflow safety | Unit | `PendingExpirationPolicyTests.handlesHugeTimeoutWithoutInstantOverflow` | Prevents `ArithmeticException` on large `Duration` values via comparison before addition. |
| **D1** | Construction validation | Unit | `PendingExpirationPolicyTests.rejectsNonPositiveOrMissingConfiguration` | Throws `IllegalArgumentException` on zero, negative, or missing timeout configuration. |
| **D2** | Precondition guard for request timestamp | Unit | `PendingExpirationPolicyTests.rejectsRequestAtOrAfterStart` | Throws `IllegalArgumentException` if $t_{\text{requested}} \ge t_{\text{screening\_start}}$. |
| **D3** | Nanosecond inclusive boundary evaluation | Unit | `PendingExpirationPolicyTests.expiresExactlyAtDeadlineWithNanosecondPrecision` | Returns `true` at $t == t_{\text{deadline}}$, `false` at $t_{\text{deadline}} - 1\text{ns}$. |
| **D3** | Non-pending state immunity | Unit | `PendingExpirationPolicyTests.neverExpiresNonPendingStates` | Returns `false` for `CONFIRMED`, `CANCELLED`, `REJECTED`, or `EXPIRED` status regardless of time. |
| Contract | Fast-fail input validation | Unit | `PendingExpirationPolicyTests.failsFastOnMissingRequiredInputs` | Throws `NullPointerException` on missing mandatory arguments. |
| **D5, D6** | Effective deadline preservation & single event | Unit | `ExpirationServiceTests.dueTransitionKeepsEffectiveDeadlineAndPublishesOnce` | `expiredAt == approvalDeadline`, exactly one response event emitted. |
| **D3** | Live hold non-mutation | Unit | `ExpirationServiceTests.livePendingDoesNotMutateOrNotify` | Status remains `PENDING_APPROVAL`, 0 events emitted. |
| **D4** | Lock-before-load execution ordering | Unit | `ExpirationServiceTests.batchObtainsLockBeforeLoadingPendingAndUsesClock` | Verifies `ScreeningRepository.findLockedById()` is called before querying pending holds. |
| **D4** | Missing screening graceful handling | Unit | `ExpirationServiceTests.missingScreeningIsNoOp` | Early exit without database queries or notification emissions. |
| **D4–D6** | Multi-seat release & sweeper idempotency | Integration | `ReservationIntegrationTests.c05RepeatedWorkerCommitsOneExpirationAndReleasesWholeHold` | Multi-seat hold fully released; second sweep execution produces zero duplicate events. |
| **D5, D6** | Transactional rollback & notification suppression | Integration | `ReservationIntegrationTests.c05RollbackRestoresPendingAndSuppressesExpirationNotification` | JPA rollback restores `PENDING_APPROVAL` and suppresses `@TransactionalEventListener(phase = AFTER_COMMIT)`. |

---

## 3. Detailed Justification of Testing Levels

### Level 1: Unit Testing Rationale

* **Example Case:** `PendingExpirationPolicyTests.expiresExactlyAtDeadlineWithNanosecondPrecision`


* **Justification:** Evaluating pure decision logic ($D3$) at nanosecond precision requires deterministic, fast execution without the overhead of Spring application context startup or database setup. In-memory unit tests allow mocking exact `Instant` inputs, ensuring the boundary condition ($t_{\text{observed}} \ge t_{\text{deadline}}$) is strictly verified without wall-clock timing drift or environment-induced flakiness.



### Level 2: Component / Integration Testing Rationale

* **Example Case:** `ReservationIntegrationTests.c05RollbackRestoresPendingAndSuppressesExpirationNotification`


* **Justification:** Isolated unit tests with mock event publishers cannot verify Spring Framework transaction boundaries or transactional listener behavior. Testing with an embedded H2 database and real Spring container is required to prove that an unhandled exception triggers a true JPA rollback (restoring `PENDING_APPROVAL` in H2) and correctly suppresses `@TransactionalEventListener(phase = AFTER_COMMIT)` notification dispatch.



---

## 4. Execution Commands & Verification Log

### Automated Execution Command (Windows PowerShell / Bash)

```powershell
# Execute complete backend test suite including C05 verification tests
cd backend
.\mvnw.cmd clean test

```

### Execution Output Summary (Executed on 2026-10-19)

```text
[INFO] Scanning for projects...
[INFO] ------------------------------------------------------------------------
[INFO] Building cinema-reservation 0.0.1-SNAPSHOT
[INFO] ------------------------------------------------------------------------
[INFO] 
[INFO] --- maven-clean-plugin:3.3.2:clean (default-clean) @ cinema-reservation ---
[INFO] 
[INFO] --- maven-surefire-plugin:3.2.5:test (default-test) @ cinema-reservation ---
[INFO] Running com.reservedbytes.cinema_reservation.CinemaReservationApplicationTests
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 2.814 s -- in com.reservedbytes.cinema_reservation.CinemaReservationApplicationTests
[INFO] Running com.reservedbytes.cinema_reservation.service.ExpirationServiceTests
[INFO] Tests run: 4, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.412 s -- in com.reservedbytes.cinema_reservation.service.ExpirationServiceTests
[INFO] Running com.reservedbytes.cinema_reservation.service.PendingExpirationPolicyTests
[INFO] Tests run: 8, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.185 s -- in com.reservedbytes.cinema_reservation.service.PendingExpirationPolicyTests
[INFO] Running com.reservedbytes.cinema_reservation.ReservationIntegrationTests
[INFO] Tests run: 61, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 11.214 s -- in com.reservedbytes.cinema_reservation.ReservationIntegrationTests
[INFO] 
[INFO] Results:
[INFO] 
[INFO] Tests run: 74, Failures: 0, Errors: 0, Skipped: 0
[INFO] 
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
[INFO] Total time:  15.025 s
[INFO] Finished at: 2026-10-19T14:22:10+02:00
[INFO] ------------------------------------------------------------------------

```

---

## 5. Architectural Dependency Verification

In addition to business rule test suites, structural compliance was validated using the automated repository architecture checker.

```powershell
# Run static architecture dependency checker
python backend/check_architecture.py .

```

**Verification Output:**

```text
PASS: 2 controller files; no direct persistence dependencies.

```

* **Constraint Verified:** Restricts REST controllers (`com.reservedbytes.cinema_reservation.controller`) from directly importing persistence packages or JPA repositories, ensuring all execution routes exclusively through domain application services.