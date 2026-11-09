package com.reservedbytes.cinema_reservation.model;

import jakarta.persistence.*;
import lombok.Getter;

@Entity
@Getter
public class CinemaHall {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false)
    private String name;

    protected CinemaHall() {}
    public CinemaHall(String name) { this.name = name; }
    public void rename(String name) { this.name = name; }
}
