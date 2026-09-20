package com.reservedbytes.cinema_reservation.repository;

import com.reservedbytes.cinema_reservation.model.Seat;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SeatRepository extends JpaRepository<Seat, Long> {}

