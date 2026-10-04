# Architecture and Decisions

## Runnable C02 v0.2

Retain Spring Boot, JPA, injected UTC Clock, H2 profiles, the React/Vite frontend
and the lightweight after-commit Notification Service. No C03 redesign.

Create still persists non-blocking DRAFT with the inclusive 15-minute boundary.
Confirm acquires all seats as either CONFIRMED or PENDING_APPROVAL. One persisted
reservation status determines the allocation of every associated seat; there is
no separate per-seat lifecycle. Approve converts the existing hold in place.

The existing pessimistic screening lock now protects Confirm, Cancel, Approve,
Reject and expiration. A scalar screening lookup occurs before loading the
reservation, avoiding stale state while waiting. Locks are held through commit;
all transitions within one screening serialize, including disjoint seats.
Availability/status also take that lock for a coherent observation. This is
intentionally simple and coarse for C02, not a scalability solution.

The demo policy selects seat numbers 3 and 4; others confirm directly. Confirm
retains required seat IDs, policy label and authorized Approver 3. Owner and
Approver checks remain separate. X-User-Id is a demo caller convention, not
trusted authentication. GET /reservations/{id} is an owner read surface; approval
uses the minimal body {decision: APPROVE|REJECT}. Both schemas are implementation
choices pending C03 API design.

A configurable 60-second demo duration yields min(request+duration, screening
start). A one-second in-process sweep and pre-read system transaction resolve
expiration. OP-02's query stays read-only and filters already-due pending holds.
Mutations re-evaluate deadline/state while holding the same lock. Domain error
responses commit any independent system expiration; no requested invalid
transition is performed. Unauthorized decisions check authority first. expiredAt
is the effective deadline. Terminal records and decision/cancellation metadata
remain observable; drafts have no TTL.

Final confirmation, cancellation, rejection and expiration publish immutable
response events; the unchanged Notification Service logs them only after commit.
Pending submission and rejected operations do not publish final confirmation.
There is no queue, external messaging provider or delivery guarantee.

## Drivers exposed for C03 (not solved here)

| Driver | Evidence / remaining design pressure |
|---|---|
| Persistent delayed approval | Requests span multiple HTTP calls; snapshot and deadline must survive deployment/restart in the eventual production store |
| Authority separate from ownership | Owner decisions fail 403; production identity trust and Resource-scoped grants need design |
| Time and expiration | UTC Clock tests equality; effective deadline and processing time differ; reliable processing and coordination across instances need design |
| Lifecycle concurrency | Confirm/Cancel/decision/expiration must share valid sequential ordering; coarse screening lock limits throughput |
| Atomic multi-seat allocation | Mixed reservation is one hold; conflict cannot acquire a free subset; durable enforcement/recovery need design |
| Stale-decision protection | Terminal old request cannot affect seats acquired by a new request; retried/duplicated commands need deliberate API semantics |
| Observable status/history | Pending progress, authority and outcome metadata are retained; production read contract/audit design remains open |
| Notifications | Only committed final transitions notify; durable delivery, retry and idempotency remain C03 responsibilities |

Production approval criteria and authority assignment, exact finite timeout,
status/decision representation and future abandoned-draft TTL remain genuine
policy/API TBDs. Reliable scheduling, persistence strategy, transaction technique,
invariant recovery and notification delivery are C03 design decisions. None
permit weakening v0.2's resolved states, authority, deadline or exclusion rules.
