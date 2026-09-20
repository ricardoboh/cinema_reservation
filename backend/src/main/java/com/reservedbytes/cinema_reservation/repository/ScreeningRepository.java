package com.reservedbytes.cinema_reservation.repository;

import com.reservedbytes.cinema_reservation.model.Screening;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ScreeningRepository extends JpaRepository<Screening, Long> {}

