# ADR-01 — Authoritative Resource Allocation under Concurrent Confirmation

**Status:** Accepted  
**Deciders:** Team Deadlock  
**Date:** 2026-10-05  
**Technical Context:** C03 Architecture Refactoring & Concurrency Safety  

---

## 1. Context & Problem Statement

In the Cinema Reservation System, concurrent `Confirm Reservation` requests may attempt to acquire the exact same physical resource `(Screening, Seat)` simultaneously. 

The domain enforces **BR-02 (Exclusive Resource Invariant)**: no two valid reservations in `CONFIRMED` or `PENDING_APPROVAL` states may hold the same resource at any point in time. Furthermore, multi-seat reservations must be allocated **atomically**—if any single requested seat is already occupied or locked by a competing hold, the entire reservation confirmation must fail without placing a partial hold on the remaining free seats.

Under high API concurrency, application-level read-before-write checks (TOCTOU — Time-of-Check to Time-of-Use) can lead to race conditions and double booking if not strictly protected.

---

## 2. Decision Drivers

* **Invariant Preservation (BR-02):** Absolute exclusion guarantee preventing duplicate active allocations across all execution paths.
* **Atomic Multi-Seat Operations:** Complete rollback of partial allocations when a conflict is encountered.
* **Scalability & Concurrency:** Minimizing long-lived database locks while maintaining strict correctness.
* **Clear Responsibility Boundaries:** Explicit separation between application workflow decisions and persistent allocation enforcement.

---

## 3. Evaluated Alternatives

### Alternative A — Application-Controlled Allocation
The application layer owns both the lifecycle logic and resource exclusivity enforcement. Exclusivity is checked via read queries (`countOtherAllocations()`) and synchronized using application-level locks (e.g., coarse pessimistic locks on the entire screening).

* **Pros:** Keeps all business logic localized within Java services; simple read queries.
* **Cons:** Vulnerable to race conditions if locking is missed in new endpoints; coarse screening locks degrade throughput by serializing unrelated seats.

### Alternative B — Database-Enforced Exclusive Allocation (Chosen)
The application manages lifecycle workflows and state transitions, while the relational database engine authoritatively enforces resource exclusivity at the persistence boundary via a dedicated `active_allocation` table with a composite unique key `UNIQUE(screening_id, seat_id)`.

* **Pros:** Database engine guarantees absolute isolation and atomicity; concurrent conflicting writes fail reliably at commit time; prevents double booking even across distributed backend nodes.
* **Cons:** Requires explicit translation of database integrity exceptions (`DataIntegrityViolationException`) into domain outcomes (`409 Conflict`); couples persistence schema to exclusivity constraints.

---

## 4. Decision Outcome

**Chosen Alternative: Alternative B — Database-Enforced Exclusive Allocation.**

The system separates architectural concerns:
1. **Reservation Management (Application):** Governs business eligibility, authority checks, and state machine transitions (`DRAFT` $\rightarrow$ `CONFIRMED` / `PENDING_APPROVAL`).
2. **Persistence Layer (Database):** Authoritatively enforces resource exclusivity using relational unique constraints on `active_allocation(screening_id, seat_id)`.

If two concurrent requests attempt to confirm overlapping seats, the relational database accepts the first transaction and rejects the second. The application layer catches the constraint violation and translates it into an `HTTP 409 Conflict` response.

---

## 5. Consequences & Mitigations

* **Positive Consequences:**
  * Eliminates application-level TOCTOU vulnerabilities for resource allocation.
  * Guarantees atomic rollback of multi-seat reservations via standard database transaction boundaries.
  * Ensures concurrency safety across horizontally scaled backend instances.

* **Negative Consequences & Mitigations:**
  * *Negative:* Database lock contention under heavy simultaneous bookings.  
    *Mitigation:* Keep transactions short and isolate active allocation rows from historical reservation logs.
  * *Negative:* Coupling to relational database features.  
    *Mitigation:* Standardize on ANSI SQL unique constraints supported by PostgreSQL, H2, and MySQL.