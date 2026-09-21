package com.reservedbytes.cinema_reservation.controller;

import com.reservedbytes.cinema_reservation.dto.*;
import com.reservedbytes.cinema_reservation.service.ReservationService;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/reservations")
public class ReservationController {
    private final ReservationService service;

    public ReservationController(ReservationService service) {
        this.service = service;
    }

    @PostMapping
    @ApiResponse(responseCode = "201", description = "Draft reservation created")
    public ResponseEntity<CreateReservationResponse> create(@RequestBody CreateReservationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request));
    }
}
