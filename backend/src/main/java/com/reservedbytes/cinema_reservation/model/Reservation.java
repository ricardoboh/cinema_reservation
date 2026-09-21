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

    protected Reservation() {}

    public Reservation(User user, Screening screening, Set<Seat> seats, Instant createdAt) {
        this.user = user;
        this.screening = screening;
        this.seats = new LinkedHashSet<>(seats);
        this.createdAt = createdAt;
        this.status = ReservationStatus.DRAFT;
    }
}

