package com.reservedbytes.cinema_reservation.model;

import jakarta.persistence.*;
import lombok.Getter;

@Entity
@Getter
public class Movie {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false)
    private String title;

    protected Movie() {}
    public Movie(String title) { this.title = title; }
    public void rename(String title) { this.title = title; }
}
