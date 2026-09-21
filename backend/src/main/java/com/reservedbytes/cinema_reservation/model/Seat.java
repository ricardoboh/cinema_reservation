package com.reservedbytes.cinema_reservation.model;

import jakarta.persistence.*;
import lombok.Getter;

@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"hall", "row_number", "seat_number"}))
@Getter
public class Seat {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false)
    private String hall;
    @Column(name = "row_number", nullable = false)
    private int rowNumber;
    @Column(name = "seat_number", nullable = false)
    private int seatNumber;

    protected Seat() {}

    public Seat(String hall, int rowNumber, int seatNumber) {
        this.hall = hall;
        this.rowNumber = rowNumber;
        this.seatNumber = seatNumber;
    }
}

