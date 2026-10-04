package com.reservedbytes.cinema_reservation.model;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;
import lombok.Getter;

@Entity
@Getter
public class Reservation {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "screening_id", nullable = false)
    private Screening screening;
    @ManyToMany
    @JoinTable(name = "reservation_seats",
        joinColumns = @JoinColumn(name = "reservation_id"),
        inverseJoinColumns = @JoinColumn(name = "seat_id"),
        uniqueConstraints = @UniqueConstraint(columnNames = {"reservation_id", "seat_id"}))
    private Set<Seat> seats = new LinkedHashSet<>();
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReservationStatus status;
    @Column(nullable = false)
    private Instant createdAt;
    private Instant cancelledAt;
    private Instant approvalRequestedAt;
    private Instant approvalDeadline;
    private Long authorizedApproverId;
    @ElementCollection
    private Set<Long> approvalRequiredSeatIds = new LinkedHashSet<>();
    private String approvalPolicy;
    private String decision;
    private Long decidedBy;
    private Instant decidedAt;
    private Instant expiredAt;

    public void requestApproval(Instant now, Instant deadline, Set<Long> requiredSeats, Long approver) {
        status = ReservationStatus.PENDING_APPROVAL;
        approvalRequestedAt = now;
        approvalDeadline = deadline;
        approvalRequiredSeatIds = new LinkedHashSet<>(requiredSeats);
        authorizedApproverId = approver;
        approvalPolicy = "C02-DEMO-v1: seat numbers 3 and 4; Approver 3";
    }

    public void decide(String decision, Long approver, Instant now) {
        this.decision = decision;
        decidedBy = approver;
        decidedAt = now;
        status = decision.equals("APPROVE") ? ReservationStatus.CONFIRMED : ReservationStatus.REJECTED;
    }

    public void expire() {
        status = ReservationStatus.EXPIRED;
        expiredAt = approvalDeadline;
    }

    public void confirm() { this.status = ReservationStatus.CONFIRMED; }

    public void cancel(Instant now) {
        this.status = ReservationStatus.CANCELLED;
        this.cancelledAt = now;
    }

    protected Reservation() {}

    public Reservation(User user, Screening screening, Set<Seat> seats, Instant createdAt) {
        this.user = user;
        this.screening = screening;
        this.seats = new LinkedHashSet<>(seats);
        this.createdAt = createdAt;
        this.status = ReservationStatus.DRAFT;
    }
}

