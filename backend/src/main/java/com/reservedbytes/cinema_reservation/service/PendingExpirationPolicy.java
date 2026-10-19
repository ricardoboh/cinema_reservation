package com.reservedbytes.cinema_reservation.service;

import com.reservedbytes.cinema_reservation.model.ReservationStatus;
import java.time.Instant;

/** Pure time decisions; callers own locking, persistence and event publication. */
public interface PendingExpirationPolicy {
    Instant deadline(Instant requestedAt, Instant screeningStart);
    boolean isDue(ReservationStatus status, Instant deadline, Instant observedAt);
}
