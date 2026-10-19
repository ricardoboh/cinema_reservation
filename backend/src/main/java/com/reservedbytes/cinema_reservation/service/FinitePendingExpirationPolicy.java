package com.reservedbytes.cinema_reservation.service;

import com.reservedbytes.cinema_reservation.model.ReservationStatus;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** Finite demo timeout; production policy remains an explicit variation point. */
@Component
public final class FinitePendingExpirationPolicy implements PendingExpirationPolicy {
    private final Duration timeout;

    public FinitePendingExpirationPolicy(
            @Value("${demo.approval-timeout:PT60S}") Duration timeout) {
        if (timeout == null || timeout.isZero() || timeout.isNegative()) {
            throw new IllegalArgumentException("Approval timeout must be positive.");
        }
        this.timeout = timeout;
    }

    @Override
    public Instant deadline(Instant requestedAt, Instant screeningStart) {
        Objects.requireNonNull(requestedAt, "requestedAt");
        Objects.requireNonNull(screeningStart, "screeningStart");
        if (!requestedAt.isBefore(screeningStart)) {
            throw new IllegalArgumentException("Approval must be requested before screening.");
        }
        // Compare durations before adding: even an enormous timeout cannot overflow Instant.
        return timeout.compareTo(Duration.between(requestedAt, screeningStart)) >= 0
                ? screeningStart : requestedAt.plus(timeout);
    }

    @Override
    public boolean isDue(ReservationStatus status, Instant deadline, Instant observedAt) {
        Objects.requireNonNull(status, "status");
        Objects.requireNonNull(observedAt, "observedAt");
        return status == ReservationStatus.PENDING_APPROVAL
                && !observedAt.isBefore(Objects.requireNonNull(deadline, "pending deadline"));
    }
}
