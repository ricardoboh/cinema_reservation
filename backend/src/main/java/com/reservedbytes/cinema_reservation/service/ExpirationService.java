package com.reservedbytes.cinema_reservation.service;

import com.reservedbytes.cinema_reservation.dto.ReservationResponse;
import com.reservedbytes.cinema_reservation.model.ReservationStatus;
import com.reservedbytes.cinema_reservation.repository.*;
import java.time.Clock;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Separate system transaction before observations; OP-02 itself stays read-only. */
@Service
public class ExpirationService {
    private final ReservationRepository reservations;
    private final ScreeningRepository screenings;
    private final Clock clock;
    private final ApplicationEventPublisher events;

    public ExpirationService(ReservationRepository reservations, ScreeningRepository screenings,
            Clock clock, ApplicationEventPublisher events) {
        this.reservations = reservations;
        this.screenings = screenings;
        this.clock = clock;
        this.events = events;
    }

    @Transactional
    public void expireScreening(Long screeningId) {
        if (screenings.findLockedById(screeningId).isPresent()) expireDueLocked(screeningId);
    }

    // Called only while the shared screening lock is held in the caller's transaction.
    public void expireDueLocked(Long screeningId) {
        var now = clock.instant();
        for (var reservation : reservations.findByScreeningIdAndStatus(screeningId, ReservationStatus.PENDING_APPROVAL)) {
            if (!now.isBefore(reservation.getApprovalDeadline())) {
                reservation.expire();
                events.publishEvent(ReservationResponse.from(reservation));
            }
        }
    }
}
