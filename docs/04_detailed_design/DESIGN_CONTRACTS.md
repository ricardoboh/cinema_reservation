# C05 — Design contracts

AS-IS baseline and source inventory: [detailed design](c05_detailed_design.md). Pure time contracts are specified in [expiration policy](EXPIRATION_POLICY.md).

## Preconditions, postconditions and failures

| Method | Preconditions / responsibility of caller | Postcondition / failure |
|---|---|---|
| `FinitePendingExpirationPolicy(Duration)` | Positive non-null duration | Immutable policy configuration; otherwise `IllegalArgumentException` |
| `deadline(requestedAt, screeningStart)` | Non-null timestamps; request strictly before start | Request < result <= start, result is capped timeout; null -> `NullPointerException`, bad order -> `IllegalArgumentException` |
| `isDue(status, deadline, observedAt)` | Non-null status/time; pending requires deadline | True iff pending and observation >= deadline; no mutation; missing required inputs -> `NullPointerException` |
| `ExpirationService.expireScreening(id)` | Invoked through Spring transactional proxy for transaction semantics | Locks screening before pending load; missing screening -> no-op; due entities updated in transaction |
| `expireDueLocked(id)` | Active transaction and shared screening write lock already held | Samples clock once and delegates to explicit-time overload |
| `expireDueLocked(id, observedAt)` | Same lock/transaction; valid observation | Loads pending entities for that screening and applies guard at the supplied instant |
| `expireIfDueLocked(reservation, observedAt)` | Managed non-null entity; active transaction and its screening write lock; valid policy inputs | Returns false without mutation/event if not due; otherwise expires and publishes one `ReservationResponse`, returns true |
| `Reservation.expire()` | Service has already checked due status/deadline | Unconditionally assigns `EXPIRED` and `expiredAt = approvalDeadline`; entity method itself validates neither state nor time |
| `ExpirationSweep.sweep()` | Spring scheduler wiring and working repositories | Enumerates distinct pending screening IDs, invokes service per ID; no outer transaction or catch/retry loop |

The `Locked` methods do not acquire locks or start transactions themselves. Their preconditions are architectural caller obligations, not runtime assertions. Repository/locking/event publication exceptions propagate. An uncaught failure can stop the current sweep before later screening IDs; prior screening transactions may already be committed.

## Lifecycle invariants and transitions

| Current state | Condition / operation | Result |
|---|---|---|
| DRAFT | Confirm requires approval | PENDING_APPROVAL, request time, deadline and approver stored |
| PENDING_APPROVAL | Observation < deadline | No expiration |
| PENDING_APPROVAL | Observation >= deadline | EXPIRED, expiredAt = deadline |
| Live PENDING_APPROVAL | Authorized APPROVE / REJECT before start | CONFIRMED / REJECTED |
| Live PENDING_APPROVAL | Valid owner cancellation before start | CANCELLED |
| Any non-pending state | Expiration guard | No mutation/event |

The service paths enforce state, authority and time rules. Entity setters are not a standalone state-machine enforcement boundary. One reservation holds all selected seats; expiration preserves those associations for history but removes the reservation from active allocation results. EXPIRED cannot be approved or cancelled through these service paths. Repeated guarded expiration cannot resurrect it or publish another expiration response.

Application-mediated conflicting transitions serialize on the screening lock. The join-table uniqueness constraint covers `(reservation_id, seat_id)`, not global `(screening, seat)` exclusivity. Direct database writers bypassing service locking are outside this guarantee.

## Transaction boundaries and notifications

`expireScreening` uses `@Transactional` (default REQUIRED), joining an existing transaction or starting one. Managed entity changes persist through dirty checking. There is no explicit save in expiration. `availability` is transactional and passes the same observation time to reconciliation and projection.

`confirm`, `cancel` and `approval` use `@Transactional(noRollbackFor = ReservationException.class)`: business rejection may still commit expiration reconciled earlier in that transaction. Other unchecked failures normally roll back. Authority/input checks can reject before any reconciliation. GET calls `resolveExpiration` first, then a separate read-only `get` transaction; there is no combined atomic read/reconcile transaction.

`ApplicationEventPublisher` publishes a `ReservationResponse` during the transaction. `NotificationService.notifyReservationChanged` uses `@TransactionalEventListener` with default AFTER_COMMIT phase and logs the response. Rollback suppresses that listener. There is no durable transport, retry or process-crash exactly-once guarantee. The one-event statement applies to successful guarded transitions under the lock.

## Contract verification

`ExpirationServiceTests` checks due/no-op behavior, ordering and one publication with mocks; it cannot prove database locks. `ReservationIntegrationTests.c05RepeatedWorkerCommitsOneExpirationAndReleasesWholeHold` checks repeat service invocation and whole-hold release. `c05RollbackRestoresPendingAndSuppressesExpirationNotification` uses `TransactionTemplate` and `setRollbackOnly()` to prove rollback and notification suppression, followed by a successful retry. See [all exact test references](c05_detailed_design.md).
