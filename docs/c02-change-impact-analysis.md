# C02 — Change Impact Analysis

## Change

Some Resources require approval by an authorized person before a
Reservation can become CONFIRMED. Approval may be delayed, rejected,
or expire.

This document analyzes the impact on Specification Baseline v0.1.
The baseline is not modified yet.

## Impact Analysis

### Create

No fundamental change is required.

Create continues to create a Reservation in DRAFT state.
No Resource allocation is created during Create.

### Availability

A new decision is required for PENDING_APPROVAL.

Proposed policy:
PENDING_APPROVAL temporarily blocks the Resource.

Rationale:
If the Resource remained available, another Reservation could become
CONFIRMED before the pending request is approved.

The block must be released when the approval request is rejected,
cancelled or expires.

### Confirm

Confirm is no longer always an immediate DRAFT -> CONFIRMED transition.

For Resources not requiring approval:

DRAFT -> CONFIRMED

For Resources requiring approval:

DRAFT -> PENDING_APPROVAL

The Reservation becomes CONFIRMED only after successful approval.

### Approve / Reject

A new actor goal and operation are required.

Actor:
Authorized Approver.

Possible outcomes:

PENDING_APPROVAL -> CONFIRMED
PENDING_APPROVAL -> REJECTED

Only an authorized Approver may perform this operation.

### Cancel

Cancellation must be extended to support:

PENDING_APPROVAL -> CANCELLED

Cancellation releases any temporary Resource block.

### Reservation State Model

The current state model is insufficient.

At minimum, PENDING_APPROVAL must be represented.

Candidate extended lifecycle:

DRAFT -> CONFIRMED
DRAFT -> PENDING_APPROVAL
PENDING_APPROVAL -> CONFIRMED
PENDING_APPROVAL -> REJECTED
PENDING_APPROVAL -> EXPIRED
PENDING_APPROVAL -> CANCELLED

Existing cancellation transitions remain applicable.

The exact representation of REJECTED and EXPIRED must be accepted by
the team before the baseline is changed.

### Use-Case Diagram

The diagram must be extended with:

- Authorized Approver actor
- Approve/Reject Reservation goal

The existing User operations remain.

### Verification

New verification scenarios are required for:

- approval-required Resource enters PENDING_APPROVAL;
- approval is delayed and Reservation remains pending;
- approval succeeds;
- approval is rejected;
- approval expires;
- cancellation while approval is pending;
- Resource availability while approval is pending;
- Resource becomes available after rejection, expiration or cancellation;
- concurrent/conflicting approval and reservation operations.

### Architecture

The change introduces new architectural drivers:

- persistent representation of an approval request/state;
- delayed processing across multiple requests;
- expiration based on time;
- possible timer/scheduled processing;
- notification of approval/rejection/expiration;
- concurrency between approval, cancellation, expiration and other Reservations.

The implementation mechanism is intentionally not selected during this
impact-analysis step.

## Conclusion

The change affects Confirm, Availability, Cancel, the Reservation
lifecycle, use cases, verification scenarios and architectural drivers.

Create can remain conceptually unchanged.

Specification Baseline v0.1 remains unchanged until the impact analysis
is accepted and the new policy decisions are made.
