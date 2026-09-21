package com.reservedbytes.cinema_reservation.repository;

import com.reservedbytes.cinema_reservation.model.*;
import java.util.Collection;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {
    @Query("""
        select count(r) from Reservation r join r.seats s
        where r.screening.id = :screeningId and r.status = :status and s.id in :seatIds
        """)
    long countConflictingSeats(@Param("screeningId") Long screeningId,
        @Param("seatIds") Collection<Long> seatIds, @Param("status") ReservationStatus status);
}

