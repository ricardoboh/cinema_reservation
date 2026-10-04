package com.reservedbytes.cinema_reservation.repository;

import com.reservedbytes.cinema_reservation.model.*;
import java.util.Collection;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {
    java.util.List<Reservation> findByScreeningIdAndStatus(Long screeningId, ReservationStatus status);

    @Query("select distinct r.screening.id from Reservation r where r.status = :status")
    java.util.List<Long> findScreeningsWithStatus(@Param("status") ReservationStatus status);

    @Query("""
        select distinct s.id from Reservation r join r.seats s
        where r.screening.id = :screeningId and
        (r.status = com.reservedbytes.cinema_reservation.model.ReservationStatus.CONFIRMED or
        (r.status = com.reservedbytes.cinema_reservation.model.ReservationStatus.PENDING_APPROVAL and r.approvalDeadline > :now))
        """)
    java.util.Set<Long> findActiveSeatIds(@Param("screeningId") Long screeningId, @Param("now") java.time.Instant now);

    @Query("""
        select count(r) from Reservation r join r.seats s
        where r.screening.id = :screeningId and r.id <> :reservationId
        and r.status in :statuses and s.id in :seatIds
        """)
    long countOtherAllocations(@Param("screeningId") Long screeningId, @Param("reservationId") Long reservationId,
        @Param("seatIds") Collection<Long> seatIds, @Param("statuses") Collection<ReservationStatus> statuses);
    @Query("select r.screening.id from Reservation r where r.id = :id")
    java.util.Optional<Long> findScreeningId(@Param("id") Long id);

    @Query("select distinct s.id from Reservation r join r.seats s where r.screening.id = :screeningId and r.status = :status")
    java.util.Set<Long> findAllocatedSeatIds(@Param("screeningId") Long screeningId, @Param("status") ReservationStatus status);
    @Query("""
        select count(r) from Reservation r join r.seats s
        where r.screening.id = :screeningId and r.status = :status and s.id in :seatIds
        """)
    long countConflictingSeats(@Param("screeningId") Long screeningId,
        @Param("seatIds") Collection<Long> seatIds, @Param("status") ReservationStatus status);
}

