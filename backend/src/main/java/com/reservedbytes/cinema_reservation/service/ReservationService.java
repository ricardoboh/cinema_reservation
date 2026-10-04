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
    private final org.springframework.context.ApplicationEventPublisher events;

    public ReservationService(ReservationRepository reservations, UserRepository users,
            ScreeningRepository screenings, SeatRepository seats, Clock clock,
            org.springframework.context.ApplicationEventPublisher events) {
        this.reservations = reservations;
        this.users = users;
        this.screenings = screenings;
        this.seats = seats;
        this.clock = clock;
        this.events = events;
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
        var now = clock.instant();
        if (now.isAfter(screening.getStartsAt().minus(Duration.ofMinutes(15)))) {
            throw new ReservationException(HttpStatus.CONFLICT, "Reservations must be created at least 15 minutes before the screening.");
        }
        var reservation = reservations.save(new Reservation(user, screening, new HashSet<>(selectedSeats), now));
        return new CreateReservationResponse(reservation.getId());
    }

    @Transactional(readOnly = true)
    public java.util.List<SeatAvailability> availability(Long screeningId) {
        var screening = screenings.findById(screeningId).orElseThrow(() ->
            new ReservationException(HttpStatus.NOT_FOUND, "Screening not found."));
        var allocated = reservations.findAllocatedSeatIds(screeningId, ReservationStatus.CONFIRMED);
        return seats.findByHallOrderByRowNumberAscSeatNumberAsc(screening.getHall()).stream()
            .map(s -> new SeatAvailability(s.getId(), s.getRowNumber(), s.getSeatNumber(),
                allocated.contains(s.getId()) ? "UNAVAILABLE" : "AVAILABLE")).toList();
    }

    @Transactional
    public ReservationResponse confirm(Long id, Long userId) {
        var reservation = lockedReservation(id, userId);
        if (reservation.getStatus() != ReservationStatus.DRAFT) {
            throw new ReservationException(HttpStatus.CONFLICT, "Only DRAFT reservations can be confirmed.");
        }
        requireBeforeStart(reservation);
        if (reservations.countConflictingSeats(reservation.getScreening().getId(),
                reservation.getSeats().stream().map(Seat::getId).toList(), ReservationStatus.CONFIRMED) > 0) {
            throw new ReservationException(HttpStatus.CONFLICT, "A selected seat is already confirmed for this screening.");
        }
        reservation.confirm();
        var response = ReservationResponse.from(reservation);
        events.publishEvent(response);
        return response;
    }

    @Transactional
    public ReservationResponse cancel(Long id, Long userId) {
        var reservation = lockedReservation(id, userId);
        if (reservation.getStatus() == ReservationStatus.CANCELLED) {
            throw new ReservationException(HttpStatus.CONFLICT, "Reservation is already CANCELLED.");
        }
        var now = clock.instant();
        if (!now.isBefore(reservation.getScreening().getStartsAt())) {
            throw new ReservationException(HttpStatus.CONFLICT, "Screening has already started.");
        }
        reservation.cancel(now);
        var response = ReservationResponse.from(reservation);
        events.publishEvent(response);
        return response;
    }

    private Reservation lockedReservation(Long id, Long userId) {
        // Scalar lookup avoids caching a stale Reservation before waiting for the lock.
        // Both transitions take this same database lock, held until transaction commit.
        var screeningId = reservations.findScreeningId(id).orElseThrow(() ->
            new ReservationException(HttpStatus.NOT_FOUND, "Reservation not found."));
        screenings.findLockedById(screeningId).orElseThrow(() ->
            new ReservationException(HttpStatus.NOT_FOUND, "Screening not found."));
        var reservation = reservations.findById(id).orElseThrow(() ->
            new ReservationException(HttpStatus.NOT_FOUND, "Reservation not found."));
        if (!reservation.getUser().getId().equals(userId)) {
            throw new ReservationException(HttpStatus.FORBIDDEN, "Only the reservation owner may perform this operation.");
        }
        return reservation;
    }

    private void requireBeforeStart(Reservation reservation) {
        if (!clock.instant().isBefore(reservation.getScreening().getStartsAt())) {
            throw new ReservationException(HttpStatus.CONFLICT, "Screening has already started.");
        }
    }
}

