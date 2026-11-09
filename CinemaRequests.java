package com.reservedbytes.cinema_reservation.dto;

import java.time.Instant;

public final class CinemaRequests {
    private CinemaRequests() {}
    public record MovieInput(String title) {}
    public record HallInput(String name) {}
    public record LayoutInput(int rows, int seatsPerRow) {}
    public record SeatInput(String seatType) {}
    public record ScreeningInput(Long movieId, Long hallId, Instant startsAt) {}
}
