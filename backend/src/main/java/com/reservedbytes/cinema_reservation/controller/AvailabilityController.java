package com.reservedbytes.cinema_reservation.controller;

import com.reservedbytes.cinema_reservation.dto.SeatAvailability;
import com.reservedbytes.cinema_reservation.service.ReservationService;
import java.util.List;
import org.springframework.web.bind.annotation.*;

@RestController
public class AvailabilityController {
    private final ReservationService service;
    public AvailabilityController(ReservationService service) { this.service = service; }

    @GetMapping("/screenings/{screeningId}/availability")
    public List<SeatAvailability> availability(@PathVariable Long screeningId) {
        return service.availability(screeningId);
    }
}
