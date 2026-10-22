package com.reservedbytes.cinema_reservation.model;

import jakarta.persistence.*;
import lombok.Getter;

@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"hall_id", "row_number", "seat_number"}))
@Getter
public class Seat {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false)
    private String hall;
@ManyToOne @JoinColumn(name="hall_id") private CinemaHall cinemaHall;
@Enumerated(EnumType.STRING) private SeatType seatType=SeatType.STANDARD;
private boolean active=true;
public enum SeatType { STANDARD, VIP }
public Seat(CinemaHall h,int r,int n){this(h.getName(),r,n);cinemaHall=h;}
public void configure(SeatType type,boolean active){seatType=type;this.active=active;}
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

