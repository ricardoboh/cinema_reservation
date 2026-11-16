# C05 — Pending expiration policy

AS-IS at `efef609615aed90aae3851d9a7e205345e862cb2`. Source: [PendingExpirationPolicy](../../backend/src/main/java/com/reservedbytes/cinema_reservation/service/PendingExpirationPolicy.java) and [FinitePendingExpirationPolicy](../../backend/src/main/java/com/reservedbytes/cinema_reservation/service/FinitePendingExpirationPolicy.java).

## Deadline computation

Let `r` be the time approval is requested by Confirm, `s` the screening start and `T` the configured timeout. The deadline is `d = min(r + T, s)`. It is not calculated from reservation creation time. Preconditions are non-null `r` and `s`, `r < s`, and a non-null positive `T`. Therefore `r < d <= s`.

The constructor reads `demo.approval-timeout`, default `PT60S`. Null, zero and negative durations throw `IllegalArgumentException`. `deadline` throws `NullPointerException` for either null timestamp and `IllegalArgumentException` for `r >= s`.

Implementation compares `T` with `Duration.between(r, s)` first. If `T` is greater or equal, it returns `s`; otherwise it adds `T` to `r`. This avoids overflowing `Instant` for a huge configured duration. Equality with the remaining time also returns `s`.

## Inclusive due guard

`isDue(status, d, observedAt)` is true exactly when `status == PENDING_APPROVAL` and `observedAt >= d`.

| Input state/time | Result |
|---|---|
| Pending, one nanosecond before deadline | false |
| Pending, exactly at deadline | true |
| Pending, after deadline | true |
| Any non-pending state, any non-null observation time | false; deadline may be null |

Status and observation time are always required. A deadline is required only for pending status because Java short-circuit evaluation skips that check for other states. Missing required inputs throw `NullPointerException`.

## Clock and execution

The policy has no clock or side effects. Callers supply `Instant` values. The application provides `Clock.systemUTC()`; tests use fixed or controlled clocks. `ExpirationService` snapshots the clock once per default batch. Availability passes its own single snapshot both to reconciliation and `findActiveSeatIds`; that query considers pending allocations active only when `approvalDeadline > now`, consistent with the inclusive expiration guard.

At the deadline the hold is logically due, but persistence changes only when a sweep or implemented request reconciliation executes. `expiredAt` records `d`, even when execution occurs later. The scheduler uses `demo.expiration-sweep-ms` for initial and fixed delay, with annotation fallback 1000 ms; the dev profile explicitly sets 1000 ms. The base YAML currently has no explicit override. No hard real-time deadline or durable scheduler is provided.

Tests: `PendingExpirationPolicyTests` contains the eight methods listed in the [design traceability table](c05_detailed_design.md). `ReservationIntegrationTests.approvalDeadlineBoundaryAndAvailability`, `availabilityCommitsExpirationAtDeadlineAndNotifiesOnce` and `pendingDeadlineIsCappedAtScreeningAndDueCancelCannotOverwriteExpiration` cover integration boundaries. These are source references, not new execution claims.
