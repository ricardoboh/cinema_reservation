# Architecture and Decisions

Architecture decisions will be documented here as the project evolves.

## C02 baseline

Reuse Spring Boot, JPA, Clock and the H2 profiles. Create persists DRAFT without
allocation checks. Availability considers only CONFIRMED for that screening.
Confirm/Cancel run in transactions and take a database pessimistic write lock on
the screening before loading reservation state. The scalar screening lookup
avoids loading stale reservation state before waiting. Locks last through commit.
This deliberately serializes transitions within each screening, including
disjoint seats, to keep BR-02 and BR-06 simple. Different screenings are independent.

Required X-User-Id headers identify callers for Confirm/Cancel; non-owners get
403. This is a baseline convention, not authentication; the specification leaves
identity transport unspecified. Notification Service logs immutable transition
events after successful commit. Rejected operations do not notify. Cancellation
retains the reservation and records cancelledAt. Draft expiration remains TBD.
