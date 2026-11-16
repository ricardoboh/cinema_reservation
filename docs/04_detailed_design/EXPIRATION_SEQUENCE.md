# C05 — Implemented expiration sequences

AS-IS components and contracts: [design](c05_detailed_design.md), [contracts](DESIGN_CONTRACTS.md). Arrows represent calls in the current Java implementation; commit notes describe Spring transaction behavior.

## Scheduled path

```mermaid
sequenceDiagram
    participant W as ExpirationSweep
    participant R as ReservationRepository
    participant E as ExpirationService
    participant S as ScreeningRepository
    participant C as Clock
    participant P as PendingExpirationPolicy
    participant D as Reservation
    participant A as ApplicationEventPublisher
    participant N as NotificationService
    W->>R: findScreeningsWithStatus(PENDING_APPROVAL)
    R-->>W: distinct screening IDs
    loop each screening ID
        W->>E: expireScreening(id)
        Note over E,S: Spring REQUIRED transaction begins or joins
        E->>S: findLockedById(id)
        alt screening exists
            S-->>E: screening with PESSIMISTIC_WRITE lock
            E->>C: instant()
            C-->>E: observedAt
            E->>R: findByScreeningIdAndStatus(id, PENDING_APPROVAL)
            R-->>E: managed pending reservations
            loop each reservation
                E->>P: isDue(status, approvalDeadline, observedAt)
                P-->>E: due
                alt due
                    E->>D: expire()
                    Note over D: status EXPIRED; expiredAt = approvalDeadline
                    E->>A: publishEvent(ReservationResponse.from(reservation))
                else live hold
                    Note over E: No mutation or event
                end
            end
        else missing screening
            S-->>E: empty Optional
            Note over E: No-op
        end
        alt transaction commits
            Note over E,R: JPA flush and commit; release lock
            A-->>N: notifyReservationChanged(response), AFTER_COMMIT if event published
        else transaction rolls back
            Note over E,N: Entity changes rolled back; listener suppressed
        end
        E-->>W: return, or propagate failure
    end
```

No per-screening exception handler is implemented: a propagated failure leaves the loop immediately. In normal scheduled execution each `expireScreening` call has its own transaction. Publication type is `ReservationResponse`; there is no `ReservationExpiredEvent` class.

## Request reconciliation paths

```mermaid
sequenceDiagram
    participant H as HTTP controllers
    participant V as ReservationService
    participant S as ScreeningRepository
    participant E as ExpirationService
    participant R as ReservationRepository
    alt reservation GET
        H->>V: resolveExpiration(id)
        V->>R: findScreeningId(id)
        opt ID found
            V->>E: expireScreening(screeningId)
            Note over E: Same transactional path as scheduled service call
        end
        H->>V: get(id, userId)
        Note over V: Separate read-only transaction and ownership check
    else availability
        H->>V: availability(screeningId)
        V->>S: findLockedById(screeningId)
        Note over V: Transaction; capture now from Clock
        V->>E: expireDueLocked(screeningId, now)
        V->>R: findActiveSeatIds(screeningId, now)
        R-->>V: active seat IDs
    else confirm, cancel or approval
        H->>V: lifecycle command
        V->>R: findScreeningId(reservationId)
        V->>S: findLockedById(screeningId)
        V->>R: findById(reservationId)
        Note over V: Authority/input checks as applicable
        V->>E: expireDueLocked(screeningId)
        Note over V: Validate state, time and applicable conflicts
        opt cancel or approval reaches final time check
            V->>E: expireIfDueLocked(reservation, now)
        end
        Note over V: Reject stale operation or perform valid lifecycle mutation
    end
```

The request diagram compresses error exits: missing references, invalid authority/input or state can stop a path before later calls. `AvailabilityController` exposes `/screenings/{screeningId}/availability`; `ReservationController` exposes the reservation paths. Due evaluation, entity mutation and event delivery inside `expireDueLocked` follow the first diagram. Business exceptions in lifecycle commands do not roll back reconciled expiration. GET and availability commit using their own transaction rules.

Traceability: `ExpirationServiceTests.batchObtainsLockBeforeLoadingPendingAndUsesClock`, `missingScreeningIsNoOp`, and the two `ReservationIntegrationTests.c05...` methods in the [design inventory](c05_detailed_design.md). Scheduler timing and recovery after process failure are not established by those tests.
