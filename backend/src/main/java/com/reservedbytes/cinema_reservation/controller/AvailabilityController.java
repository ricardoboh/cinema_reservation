package com.reservedbytes.cinema_reservation.controller;

import com.reservedbytes.cinema_reservation.dto.SeatAvailability;
import com.reservedbytes.cinema_reservation.service.ReservationService;
import java.util.List;
import org.springframework.web.bind.annotation.*;

@RestController
public class AvailabilityController {
    private final ReservationService service;
    private final com.reservedbytes.cinema_reservation.service.ExpirationService expiration;
    public AvailabilityController(ReservationService service, com.reservedbytes.cinema_reservation.service.ExpirationService expiration) {
        this.service = service;
        this.expiration = expiration;
    }

    @GetMapping("/screenings/{screeningId}/availability")
    public List<SeatAvailability> availability(@PathVariable Long screeningId) {
        expiration.expireScreening(screeningId);
        return service.availability(screeningId);
    }
}
