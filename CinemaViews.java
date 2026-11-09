package com.reservedbytes.cinema_reservation.dto;

import java.time.Instant;

public final class CinemaViews {
    private CinemaViews() {}
    public record MovieView(Long id, String title) {}
    public record HallView(Long id, String name) {}
    public record SeatView(Long id, int rowNumber, int seatNumber, String seatType) {}
    public record ScreeningView(Long id, MovieView movie, HallView hall, Instant startsAt) {}
}
