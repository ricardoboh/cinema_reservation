package com.reservedbytes.cinema_reservation.service;

import com.reservedbytes.cinema_reservation.dto.ReservationResponse;
import com.reservedbytes.cinema_reservation.model.Reservation;
import com.reservedbytes.cinema_reservation.model.ReservationStatus;
import com.reservedbytes.cinema_reservation.repository.*;
import java.time.Clock;
import java.time.Instant;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Central expiration transitions, shared by observations, commands and the sweep. */
@Service
public class ExpirationService {
    private final ReservationRepository reservations;
    private final ScreeningRepository screenings;
    private final Clock clock;
    private final PendingExpirationPolicy policy;
    private final ApplicationEventPublisher events;

    public ExpirationService(ReservationRepository reservations, ScreeningRepository screenings,
            Clock clock, ApplicationEventPublisher events, PendingExpirationPolicy policy) {
        this.reservations = reservations;
        this.screenings = screenings;
        this.clock = clock;
        this.policy = policy;
        this.events = events;
    }

    @Transactional
    public void expireScreening(Long screeningId) {
        if (screenings.findLockedById(screeningId).isPresent()) expireDueLocked(screeningId);
    }

    // Called only while the shared screening lock is held in the caller's transaction.
    public void expireDueLocked(Long screeningId) {
        expireDueLocked(screeningId, clock.instant());
    }

    // Availability uses the same observation time for reconciliation and projection.
    public void expireDueLocked(Long screeningId, Instant observedAt) {
        for (var reservation : reservations.findByScreeningIdAndStatus(screeningId, ReservationStatus.PENDING_APPROVAL)) {
            expireIfDueLocked(reservation, observedAt);
        }
    }

    /** Requires an active transaction and the reservation's screening lock. */
    public boolean expireIfDueLocked(Reservation reservation, Instant observedAt) {
        if (!policy.isDue(reservation.getStatus(), reservation.getApprovalDeadline(), observedAt)) return false;
        reservation.expire();
        events.publishEvent(ReservationResponse.from(reservation));
        return true;
    }
}
