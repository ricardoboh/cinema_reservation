package com.reservedbytes.cinema_reservation.repository;

import com.reservedbytes.cinema_reservation.model.Screening;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ScreeningRepository extends JpaRepository<Screening, Long> {
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select s from Screening s where s.id = :id")
    java.util.Optional<Screening> findLockedById(@org.springframework.data.repository.query.Param("id") Long id);
}

