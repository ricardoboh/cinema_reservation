package com.reservedbytes.cinema_reservation.dto;

public record SeatAvailability(Long seatId, int rowNumber, int seatNumber, String availability) {}
