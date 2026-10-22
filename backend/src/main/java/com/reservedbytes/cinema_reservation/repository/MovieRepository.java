package com.reservedbytes.cinema_reservation.repository;
import com.reservedbytes.cinema_reservation.model.Movie;
import org.springframework.data.jpa.repository.*;
public interface MovieRepository extends JpaRepository<Movie,Long>  {
    @Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE) @Query("select x from Movie x where x.id=:id") java.util.Optional<Movie> lockById(@org.springframework.data.repository.query.Param("id") Long id);
}
