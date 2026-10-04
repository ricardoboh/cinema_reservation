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

    @PostMapping("/{id}/confirm")
    public ReservationResponse confirm(@PathVariable Long id, @RequestHeader("X-User-Id") Long userId) {
        return service.confirm(id, userId);
    }

    @GetMapping("/{id}")
    public ReservationResponse get(@PathVariable Long id, @RequestHeader("X-User-Id") Long userId) {
        service.resolveExpiration(id);
        return service.get(id, userId);
    }

    @PostMapping("/{id}/approval")
    public ReservationResponse approval(@PathVariable Long id, @RequestHeader("X-User-Id") Long approverId,
            @RequestBody ApprovalRequest request) {
        return service.approval(id, approverId, request);
    }

    @PostMapping("/{id}/cancel")
    public ReservationResponse cancel(@PathVariable Long id, @RequestHeader("X-User-Id") Long userId) {
        return service.cancel(id, userId);
    }
}
