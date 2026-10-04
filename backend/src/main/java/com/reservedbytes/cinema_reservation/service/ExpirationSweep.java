package com.reservedbytes.cinema_reservation.service;

import com.reservedbytes.cinema_reservation.model.ReservationStatus;
import com.reservedbytes.cinema_reservation.repository.ReservationRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Best-effort local sweep. Correctness also checks deadlines under the database lock. */
@Component
public class ExpirationSweep {
    private final ReservationRepository reservations;
    private final ExpirationService expiration;
    public ExpirationSweep(ReservationRepository reservations, ExpirationService expiration) {
        this.reservations = reservations;
        this.expiration = expiration;
    }
    @Scheduled(fixedDelayString = "${demo.expiration-sweep-ms:1000}", initialDelayString = "${demo.expiration-sweep-ms:1000}")
    public void sweep() {
        for (var id : reservations.findScreeningsWithStatus(ReservationStatus.PENDING_APPROVAL)) {
            expiration.expireScreening(id);
        }
    }
}
