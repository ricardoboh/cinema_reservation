package com.reservedbytes.cinema_reservation.repository;
import com.reservedbytes.cinema_reservation.model.CinemaHall;
import org.springframework.data.jpa.repository.*;
public interface CinemaHallRepository extends JpaRepository<CinemaHall,Long>  {
    @Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE) @Query("select x from CinemaHall x where x.id=:id") java.util.Optional<CinemaHall> lockById(@org.springframework.data.repository.query.Param("id") Long id);
}
