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
    private final ExpirationService expiration;
    private final ApprovalPolicy policy;
    private final Duration approvalTimeout;

    public ReservationService(ReservationRepository reservations, UserRepository users,
            ScreeningRepository screenings, SeatRepository seats, Clock clock,
            org.springframework.context.ApplicationEventPublisher events, ExpirationService expiration,
            ApprovalPolicy policy, @org.springframework.beans.factory.annotation.Value("${demo.approval-timeout:PT60S}") Duration approvalTimeout) {
        this.reservations = reservations;
        this.users = users;
        this.screenings = screenings;
        this.seats = seats;
        this.clock = clock;
        this.events = events;
        this.expiration = expiration;
        this.policy = policy;
        if (approvalTimeout.isZero() || approvalTimeout.isNegative()) throw new IllegalArgumentException("Demo approval timeout must be positive.");
        this.approvalTimeout = approvalTimeout;
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
        var screening = screenings.findLockedById(screeningId).orElseThrow(() ->
            new ReservationException(HttpStatus.NOT_FOUND, "Screening not found."));
        var allocated = reservations.findActiveSeatIds(screeningId, clock.instant());
        return seats.findByHallOrderByRowNumberAscSeatNumberAsc(screening.getHall()).stream()
            .map(s -> new SeatAvailability(s.getId(), s.getRowNumber(), s.getSeatNumber(),
                allocated.contains(s.getId()) ? "UNAVAILABLE" : "AVAILABLE", policy.requiresApproval(s))).toList();
    }

    @Transactional(noRollbackFor = ReservationException.class)
    public ReservationResponse confirm(Long id, Long userId) {
        var reservation = lockedReservation(id, userId);
        expiration.expireDueLocked(reservation.getScreening().getId());
        if (reservation.getStatus() != ReservationStatus.DRAFT) {
            throw new ReservationException(HttpStatus.CONFLICT, "Only DRAFT reservations can be confirmed.");
        }
        requireBeforeStart(reservation);
        if (hasOtherAllocation(reservation)) {
            throw new ReservationException(HttpStatus.CONFLICT, "A selected seat is already held or confirmed for this screening.");
        }
        var required = reservation.getSeats().stream().filter(policy::requiresApproval)
            .map(Seat::getId).collect(java.util.stream.Collectors.toSet());
        var now = clock.instant();
        if (!now.isBefore(reservation.getScreening().getStartsAt())) {
            throw new ReservationException(HttpStatus.CONFLICT, "Screening has already started.");
        }
        if (required.isEmpty()) reservation.confirm();
        else {
            var deadline = now.plus(approvalTimeout);
            if (deadline.isAfter(reservation.getScreening().getStartsAt())) deadline = reservation.getScreening().getStartsAt();
            reservation.requestApproval(now, deadline, required, ApprovalPolicy.DEMO_APPROVER_ID);
        }
        var response = ReservationResponse.from(reservation);
        if (reservation.getStatus() == ReservationStatus.CONFIRMED) events.publishEvent(response);
        return response;
    }

    @Transactional(noRollbackFor = ReservationException.class)
    public ReservationResponse cancel(Long id, Long userId) {
        var reservation = lockedReservation(id, userId);
        expiration.expireDueLocked(reservation.getScreening().getId());
        if (!java.util.Set.of(ReservationStatus.DRAFT, ReservationStatus.PENDING_APPROVAL, ReservationStatus.CONFIRMED).contains(reservation.getStatus())) {
            throw new ReservationException(HttpStatus.CONFLICT, "Only DRAFT, PENDING_APPROVAL or CONFIRMED reservations can be cancelled.");
        }
        var now = clock.instant();
        expireAtIfDue(reservation, now);
        if (reservation.getStatus() == ReservationStatus.EXPIRED) {
            throw new ReservationException(HttpStatus.CONFLICT, "Approval deadline has been reached.");
        }
        if (!now.isBefore(reservation.getScreening().getStartsAt())) {
            throw new ReservationException(HttpStatus.CONFLICT, "Screening has already started.");
        }
        reservation.cancel(now);
        var response = ReservationResponse.from(reservation);
        events.publishEvent(response);
        return response;
    }

    private Reservation lockedReservation(Long id, Long userId) {
        var reservation = lockedReservation(id);
        if (!reservation.getUser().getId().equals(userId)) {
            throw new ReservationException(HttpStatus.FORBIDDEN, "Only the reservation owner may perform this operation.");
        }
        return reservation;
    }

    private Reservation lockedReservation(Long id) {
        // Scalar lookup avoids caching a stale Reservation before waiting for the lock.
        // Both transitions take this same database lock, held until transaction commit.
        var screeningId = reservations.findScreeningId(id).orElseThrow(() ->
            new ReservationException(HttpStatus.NOT_FOUND, "Reservation not found."));
        screenings.findLockedById(screeningId).orElseThrow(() ->
            new ReservationException(HttpStatus.NOT_FOUND, "Screening not found."));
        var reservation = reservations.findById(id).orElseThrow(() ->
            new ReservationException(HttpStatus.NOT_FOUND, "Reservation not found."));
        return reservation;
    }

    public void resolveExpiration(Long id) {
        reservations.findScreeningId(id).ifPresent(expiration::expireScreening);
    }

    @Transactional(readOnly = true)
    public ReservationResponse get(Long id, Long userId) {
        return ReservationResponse.from(lockedReservation(id, userId));
    }

    @Transactional(noRollbackFor = ReservationException.class)
    public ReservationResponse approval(Long id, Long approverId, ApprovalRequest request) {
        var reservation = lockedReservation(id);
        // Check authority before any mutation. Owner status never grants this authority.
        var authorized = reservation.getAuthorizedApproverId();
        if (!approverId.equals(authorized == null ? ApprovalPolicy.DEMO_APPROVER_ID : authorized)) {
            throw new ReservationException(HttpStatus.FORBIDDEN, "Only the authorized demo Approver may decide this request.");
        }
        if (request == null || !java.util.Set.of("APPROVE", "REJECT").contains(request.decision() == null ? "" : request.decision())) {
            throw new ReservationException(HttpStatus.BAD_REQUEST, "Decision must be APPROVE or REJECT.");
        }
        expiration.expireDueLocked(reservation.getScreening().getId());
        if (reservation.getStatus() != ReservationStatus.PENDING_APPROVAL) {
            throw new ReservationException(HttpStatus.CONFLICT, "Only a live PENDING_APPROVAL reservation can be decided.");
        }
        requireBeforeStart(reservation);
        if (hasOtherAllocation(reservation)) {
            throw new ReservationException(HttpStatus.CONFLICT, "Exclusive pending hold invariant failed; investigation required.");
        }
        var now = clock.instant();
        expireAtIfDue(reservation, now);
        if (reservation.getStatus() == ReservationStatus.EXPIRED) {
            throw new ReservationException(HttpStatus.CONFLICT, "Approval deadline has been reached.");
        }
        reservation.decide(request.decision(), approverId, now);
        var response = ReservationResponse.from(reservation);
        events.publishEvent(response);
        return response;
    }

    private boolean hasOtherAllocation(Reservation reservation) {
        return reservations.countOtherAllocations(reservation.getScreening().getId(), reservation.getId(),
            reservation.getSeats().stream().map(Seat::getId).toList(),
            java.util.Set.of(ReservationStatus.PENDING_APPROVAL, ReservationStatus.CONFIRMED)) > 0;
    }

    private void expireAtIfDue(Reservation reservation, java.time.Instant now) {
        if (reservation.getStatus() == ReservationStatus.PENDING_APPROVAL && !now.isBefore(reservation.getApprovalDeadline())) {
            reservation.expire();
            events.publishEvent(ReservationResponse.from(reservation));
        }
    }

    private void requireBeforeStart(Reservation reservation) {
        if (!clock.instant().isBefore(reservation.getScreening().getStartsAt())) {
            throw new ReservationException(HttpStatus.CONFLICT, "Screening has already started.");
        }
    }
}

