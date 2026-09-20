package com.reservedbytes.cinema_reservation.model;

import jakarta.persistence.*;
import java.time.Instant;
import lombok.Getter;

@Entity
@Getter
public class Screening {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false)
    private String movie;
    @Column(nullable = false)
    private String hall;
    @Column(nullable = false)
    private Instant startsAt;

    protected Screening() {}

    public Screening(String movie, String hall, Instant startsAt) {
        this.movie = movie;
        this.hall = hall;
        this.startsAt = startsAt;
    }
}

