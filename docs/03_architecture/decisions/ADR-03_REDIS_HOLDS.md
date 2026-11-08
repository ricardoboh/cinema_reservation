# ADR-03 — Distributed In-Memory Cache (Redis) for Seat Holds

**Status:** Rejected  
**Deciders:** Team Deadlock  
**Date:** 2026-10-05  
**Technical Context:** Temporary Seat Reservation Locks & High-Throughput Hold Management  

---

## 1. Context & Problem Statement

During the specification of Baseline v0.2, the system introduced the `PENDING_APPROVAL` state, where seats must be locked temporarily while awaiting managerial authorization or expiration timeouts. 

Because seat selection and availability queries (`GET /screenings/{id}/availability`) generate high read/write throughput, the team evaluated using a distributed in-memory cache (**Redis**) to store temporary seat holds separately from the primary relational database.

---

## 2. Decision Drivers

* **Performance & Latency:** Ultra-low latency responses for seat availability queries.
* **Automatic Expiration:** Built-in TTL (Time-To-Live) capabilities in Redis for automatic hold expiration without database background sweepers.
* **Database Offloading:** Reducing write contention and lock overhead on the main relational database.
* **Operational Simplicity:** Minimizing infrastructure complexity and deployment overhead for local and CI environments.

---

## 3. Evaluated Alternatives

### Alternative A — In-Memory Distributed Cache (Redis)
Store temporary seat holds (`PENDING_APPROVAL`) strictly inside a Redis cache cluster with active TTLs. Upon expiration, Redis automatically evicts the key, releasing the seat. Once approved, the hold is migrated to the relational database as a `CONFIRMED` reservation.

### Alternative B — Unified Relational Database Storage (Selected in Architecture)
Store all reservation states (`DRAFT`, `PENDING_APPROVAL`, `CONFIRMED`, `CANCELLED`, `REJECTED`, `EXPIRED`) and seat allocations strictly inside the primary relational database, using transaction boundaries and SQL constraints.

---

## 4. Reason for Rejection (Alternative A)

While Redis offers native TTL expiration and high throughput, Alternative A was **rejected** for the following architectural reasons:

1. **Dual-Source Invariant Violations (Dual-Write Risk):**  
   Splitting active allocations between Redis (holds) and Postgres/H2 (confirmed seats) breaks atomic transactional boundaries. Enforcing **BR-02 (Exclusive Resource Invariant)** across two separate data stores introduces potential double-booking race conditions if cache sync fails or lags.

2. **Lack of Durable Audit History:**  
   When a key expires in Redis, it is evicted without preserving observable domain history. Baseline v0.2 requires retaining terminal records (`EXPIRED`, `REJECTED`) alongside decision timestamps and actor metadata for reporting and customer support.

3. **Infrastructure & Deployment Complexity:**  
   Introducing Redis adds an external service dependency, increasing local development, testing, and CI pipeline setup overhead without a immediate scalability bottleneck in the core cinema domain.

---

## 5. Architectural Outcome

The team decided to maintain a **single source of truth** within the relational database engine. Temporary holds (`PENDING_APPROVAL`) and final allocations (`CONFIRMED`) are managed within the same relational schema (`active_allocation` table), protected by relational unique keys (`ADR-01`) and background sweepers (`ADR-02`).