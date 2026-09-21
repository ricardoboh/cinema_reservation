package com.reservedbytes.cinema_reservation.repository;

import com.reservedbytes.cinema_reservation.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {}

