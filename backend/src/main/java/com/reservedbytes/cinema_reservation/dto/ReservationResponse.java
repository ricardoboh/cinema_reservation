package com.reservedbytes.cinema_reservation.dto;

import com.reservedbytes.cinema_reservation.model.*;
import java.time.Instant;
import java.util.List;

public record ReservationResponse(Long id, Long userId, Long screeningId, List<Long> seatIds,
        ReservationStatus status, Instant createdAt, Instant cancelledAt,
        Instant approvalRequestedAt, Instant approvalDeadline, String approvalPolicy,
        List<Long> approvalRequiredSeatIds, Long authorizedApproverId,
        String decision, Long decidedBy, Instant decidedAt, Instant expiredAt, String movieTitle, String hallName, Instant startsAt) {
    public static ReservationResponse from(Reservation r) {
        return new ReservationResponse(r.getId(), r.getUser().getId(), r.getScreening().getId(),
            r.getSeats().stream().map(Seat::getId).sorted().toList(), r.getStatus(), r.getCreatedAt(), r.getCancelledAt(),
            r.getApprovalRequestedAt(), r.getApprovalDeadline(), r.getApprovalPolicy(),
            r.getApprovalRequiredSeatIds().stream().sorted().toList(), r.getAuthorizedApproverId(),
            r.getDecision(), r.getDecidedBy(), r.getDecidedAt(), r.getExpiredAt(), r.getScreening().getMovieEntity()==null?r.getScreening().getMovie():r.getScreening().getMovieEntity().getTitle(), r.getScreening().getCinemaHall()==null?r.getScreening().getHall():r.getScreening().getCinemaHall().getName(), r.getScreening().getStartsAt());
    }
}
