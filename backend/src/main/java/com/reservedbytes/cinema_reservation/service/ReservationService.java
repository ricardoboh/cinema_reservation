package com.reservedbytes.cinema_reservation.service;

import com.reservedbytes.cinema_reservation.dto.*;
import com.reservedbytes.cinema_reservation.exception.ReservationException;
import com.reservedbytes.cinema_reservation.model.*;
import com.reservedbytes.cinema_reservation.repository.*;
import java.time.Clock;
import java.time.Duration;
import java.util.HashSet;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReservationService {
    private final ReservationRepository reservations;
    private final UserRepository users;
    private final ScreeningRepository screenings;
    private final SeatRepository seats;
    private final Clock clock;

    public ReservationService(ReservationRepository reservations, UserRepository users,
            ScreeningRepository screenings, SeatRepository seats, Clock clock) {
        this.reservations = reservations;
        this.users = users;
        this.screenings = screenings;
        this.seats = seats;
        this.clock = clock;
    }

    @Transactional
    public CreateReservationResponse create(CreateReservationRequest request) {
        if (request == null || request.userId() == null || request.userId() <= 0
                || request.screeningId() == null || request.screeningId() <= 0
                || request.seatIds() == null || request.seatIds().isEmpty()
                || request.seatIds().stream().anyMatch(id -> id == null || id <= 0)
                || new HashSet<>(request.seatIds()).size() != request.seatIds().size()) {
            throw new ReservationException(HttpStatus.BAD_REQUEST,
                "Positive userId, screeningId and a non-empty list of distinct positive seatIds are required.");
        }
        var user = users.findById(request.userId()).orElseThrow(() ->
            new ReservationException(HttpStatus.NOT_FOUND, "User not found."));
        var screening = screenings.findById(request.screeningId()).orElseThrow(() ->
            new ReservationException(HttpStatus.NOT_FOUND, "Screening not found."));
        var selectedSeats = seats.findAllById(request.seatIds());
        if (selectedSeats.size() != request.seatIds().size()) {
            throw new ReservationException(HttpStatus.NOT_FOUND, "Seat not found.");
        }
        if (selectedSeats.stream().anyMatch(seat -> !seat.getHall().equals(screening.getHall()))) {
            throw new ReservationException(HttpStatus.BAD_REQUEST, "Seats must belong to the screening hall.");
        }
        if (reservations.countConflictingSeats(screening.getId(), request.seatIds(),
                ReservationStatus.CONFIRMED) > 0) {
            throw new ReservationException(HttpStatus.CONFLICT, "A selected seat is already confirmed for this screening.");
        }
        var now = clock.instant();
        if (now.isAfter(screening.getStartsAt().minus(Duration.ofMinutes(15)))) {
            throw new ReservationException(HttpStatus.CONFLICT, "Reservations must be created at least 15 minutes before the screening.");
        }
        var reservation = reservations.save(new Reservation(user, screening, new HashSet<>(selectedSeats), now));
        return new CreateReservationResponse(reservation.getId());
    }
}

