# ADR-02 — Transactional Outbox Pattern for Lifecycle Notifications

**Status:** Accepted  
**Deciders:** Team Deadlock  
**Date:** 2026-10-05  
**Technical Context:** Asynchronous Event Messaging & External System Integration  

---

## 1. Context & Problem Statement

The system integration boundary requires dispatching lifecycle notifications to an external **Notification Service** whenever a reservation transitions into a final outcome state (`CONFIRMED`, `CANCELLED`, `REJECTED`, or `EXPIRED`).

Calling an external HTTP API directly inside the database transaction introduces serious architectural risks:
1. **Dual-Write Problem:** If the external call succeeds but the local database transaction rolls back, an invalid notification is sent.
2. **Availability Coupling:** If the external Notification Service is down or slow, application transactions stall or fail, preventing users from making valid bookings.

---

## 2. Decision Drivers

* **Transactional Consistency:** Notifications must be initiated **only** for successfully committed business state changes (BR-09).
* **System Resiliency:** External service outages must not block local database commits or user booking workflows.
* **At-Least-Once Delivery:** Reliable background dispatch with persistent retry tracking.
* **Non-Blocking Execution:** Decoupling client HTTP response times from external network latency.

---

## 3. Evaluated Alternatives

### Alternative A — Direct Synchronous Call Inside Transaction
Execute synchronous HTTP calls to the Notification Service directly inside `ReservationService` before committing the transaction.

* **Pros:** Simple to implement; no background workers or extra database tables required.
* **Cons:** Violates dual-write safety; network timeouts cause database connection pool exhaustion and transaction rollbacks.

### Alternative B — In-Memory Event Listener (`@TransactionalEventListener`)
Publish Spring application events and consume them after commit using `@TransactionalEventListener`.

* **Pros:** Native Spring support; separates notification code from core business services.
* **Cons:** In-memory events are lost if the application crashes or restarts immediately post-commit before dispatching the network call.

### Alternative C — Transactional Outbox Pattern (Chosen)
Persist an immutable notification event into an `outbox_events` database table within the **same local database transaction** that updates the reservation state. A separate background worker periodically reads pending outbox entries, dispatches them to the external service, and marks them as sent.

* **Pros:** Guaranteed atomic alignment between database state and notification records; survives process crashes; allows configurable retries and exponential backoff.
* **Cons:** Requires an additional database table, polling worker/scheduler, and event deduplication mechanics.

---

## 4. Decision Outcome

**Chosen Alternative: Alternative C — Transactional Outbox Pattern.**

All state-changing operations (`Confirm`, `Cancel`, `Approve`, `Reject`, `Expire`) insert a JSON-serialized event into the `outbox_events` table as part of their atomic business transaction. 

A dedicated asynchronous component (**Notification Integration**) polls committed outbox records, dispatches them with a unique `eventId`, and updates dispatch status upon receipt of an HTTP acknowledgement.

---

## 5. Consequences & Mitigations

* **Positive Consequences:**
  * Complete decoupling of business transaction success from third-party API availability.
  * Outbound notifications strictly match persisted database states.
  * Undelivered notifications survive JVM restarts and are retried automatically.

* **Negative Consequences & Mitigations:**
  * *Negative:* Potential duplicate sends if the process crashes after API delivery but before updating the outbox status.  
    *Mitigation:* Include a unique, deterministic `eventId` in the payload to allow the external Notification Service to deduplicate messages.
  * *Negative:* Polling overhead on the relational database.  
    *Mitigation:* Index outbox records by `(status, next_attempt_at)` and process events in optimized batches.