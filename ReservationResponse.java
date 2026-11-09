package com.reservedbytes.cinema_reservation.dto;

import com.reservedbytes.cinema_reservation.model.Reservation;
import com.reservedbytes.cinema_reservation.model.ReservationStatus;
import com.reservedbytes.cinema_reservation.model.Seat;
import java.time.Instant;
import java.util.List;

public record ReservationResponse(
        Long reservationId, Long userId, Long screeningId, List<Long> seats,
        ReservationStatus status, Instant createdAt, Instant cancelledAt,
        Instant approvalRequestedAt, Instant approvalDeadline, String approvalPolicy,
        List<Long> approvalRequiredSeatIds, Long authorizedApproverId,
        String decision, Long approverId, Instant decisionAt, Instant expiredAt,
        String movieTitle, String hallName, Instant startsAt) {

    public static ReservationResponse from(Reservation reservation) {
        var screening = reservation.getScreening();
        return new ReservationResponse(
            reservation.getId(), reservation.getUser().getId(), screening.getId(),
            reservation.getSeats().stream().map(Seat::getId).sorted().toList(),
            reservation.getStatus(), reservation.getCreatedAt(), reservation.getCancelledAt(),
            reservation.getApprovalRequestedAt(), reservation.getApprovalDeadline(),
            reservation.getApprovalPolicy(),
            reservation.getApprovalRequiredSeatIds().stream().sorted().toList(),
            reservation.getAuthorizedApproverId(), reservation.getDecision(),
            reservation.getDecidedBy(), reservation.getDecidedAt(), reservation.getExpiredAt(),
            screening.getMovieEntity() == null ? screening.getMovie() : screening.getMovieEntity().getTitle(),
            screening.getCinemaHall() == null ? screening.getHall() : screening.getCinemaHall().getName(),
            screening.getStartsAt());
    }
}
