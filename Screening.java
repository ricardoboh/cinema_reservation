package com.reservedbytes.cinema_reservation.model;

import jakarta.persistence.*;
import java.time.Instant;
import lombok.Getter;

@Entity
@Getter
public class Screening {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    // Text references keep previously persisted baseline records readable.
    @Column(nullable = false)
    private String movie;
    @Column(nullable = false)
    private String hall;
    @ManyToOne @JoinColumn(name = "movie_id")
    private Movie movieEntity;
    @ManyToOne @JoinColumn(name = "hall_id")
    private CinemaHall cinemaHall;
    @Column(nullable = false, updatable = false, columnDefinition = "timestamp(9) with time zone")
    private Instant startsAt;

    protected Screening() {}

    public Screening(Movie movie, CinemaHall hall, Instant startsAt) {
        this(movie.getTitle(), hall.getName(), startsAt);
        this.movieEntity = movie;
        this.cinemaHall = hall;
    }

    public Screening(String movie, String hall, Instant startsAt) {
        this.movie = movie;
        this.hall = hall;
        this.startsAt = startsAt;
    }
}
