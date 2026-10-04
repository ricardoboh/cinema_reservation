package com.reservedbytes.cinema_reservation.service;

import com.reservedbytes.cinema_reservation.dto.ReservationResponse;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionalEventListener;

/** Lightweight notification boundary: delivered only after a successful commit. */
@Service
public class NotificationService {
    @TransactionalEventListener
    public void notifyReservationChanged(ReservationResponse reservation) {
        LoggerFactory.getLogger(NotificationService.class).info("Notification: reservation {} for user {} is {}",
            reservation.id(), reservation.userId(), reservation.status());
    }
}
