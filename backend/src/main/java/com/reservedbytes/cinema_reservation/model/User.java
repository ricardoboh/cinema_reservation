package com.reservedbytes.cinema_reservation.model;

import jakarta.persistence.*;
import lombok.Getter;

@Entity
@Table(name = "cinema_users")
@Getter
public class User {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    public User() {}
}

