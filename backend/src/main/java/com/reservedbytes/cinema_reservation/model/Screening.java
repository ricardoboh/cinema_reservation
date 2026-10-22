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
@ManyToOne @JoinColumn(name="movie_id") private Movie movieEntity;
@ManyToOne @JoinColumn(name="hall_id") private CinemaHall cinemaHall;
private Instant endsAt; private boolean cancelled=false;
public Screening(Movie m,CinemaHall h,Instant start){configure(m,h,start);}
public void configure(Movie m,CinemaHall h,Instant start){movieEntity=m;cinemaHall=h;movie=m.getTitle();hall=h.getName();startsAt=start;endsAt=start.plusSeconds(m.getDurationMinutes()*60L);}
public void cancelScreening(){cancelled=true;}

    protected Screening() {}

    public Screening(String movie, String hall, Instant startsAt) {
        this.movie = movie;
        this.hall = hall;
        this.startsAt = startsAt;
    }
}

