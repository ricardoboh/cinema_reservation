package com.reservedbytes.cinema_reservation.model;

import jakarta.persistence.*;
import lombok.Getter;

@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"hall_id", "row_number", "seat_number"}))
@Getter
public class Seat {
    public enum SeatType { STANDARD, VIP }

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false)
    private String hall;
    @ManyToOne @JoinColumn(name = "hall_id")
    private CinemaHall cinemaHall;
    @Column(name = "row_number", nullable = false)
    private int rowNumber;
    @Column(name = "seat_number", nullable = false)
    private int seatNumber;
    @Enumerated(EnumType.STRING) @Column(nullable = false)
    private SeatType seatType = SeatType.STANDARD;

    protected Seat() {}
    public Seat(String hall, int rowNumber, int seatNumber) {
        this.hall = hall;
        this.rowNumber = rowNumber;
        this.seatNumber = seatNumber;
    }
    public Seat(CinemaHall hall, int rowNumber, int seatNumber) {
        this(hall.getName(), rowNumber, seatNumber);
        this.cinemaHall = hall;
    }
    public void classify(SeatType seatType) { this.seatType = seatType; }
}
