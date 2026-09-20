package com.reservedbytes.cinema_reservation.dto;

import java.util.List;

public record CreateReservationRequest(Long userId, Long screeningId, List<Long> seatIds) {}

