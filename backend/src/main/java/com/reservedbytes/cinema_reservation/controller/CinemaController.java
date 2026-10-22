package com.reservedbytes.cinema_reservation.controller;
import com.reservedbytes.cinema_reservation.service.CinemaManagement;
import com.reservedbytes.cinema_reservation.service.CinemaManagement.*;
import com.reservedbytes.cinema_reservation.model.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;
import java.util.*;
@RestController @Transactional public class CinemaController  {
    private final CinemaManagement c;
    private final Clock clock;
    public CinemaController(CinemaManagement c,Clock clock) {
        this.c=c;
        this.clock=clock;
    }
    @GetMapping("/movies") public List<Movie> movies() {
        return c.movies().findAll();
    }
    @GetMapping("/movies/{id}") public Movie movie(@PathVariable Long id) {
        return c.found(c.movies().findById(id));
    }
    @PostMapping("/movies") @ResponseStatus(HttpStatus.CREATED) public Movie addMovie(@RequestBody MovieInput x) {
        return c.saveMovie(null,x);
    }
    @PutMapping("/movies/{id}") public Movie editMovie(@PathVariable Long id,@RequestBody MovieInput x) {
        return c.saveMovie(id,x);
    }
    @PostMapping("/movies/{id}/deactivate") public Movie deactivate(@PathVariable Long id) {
        Movie m=c.found(c.movies().lockById(id));
        m.setActive(false);
        return m;
    }
    @DeleteMapping("/movies/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) public void delete(@PathVariable Long id) {
        c.deleteMovie(id);
    }
    @GetMapping("/halls") public List<CinemaHall> halls() {
        return c.halls().findAll();
    }
    @GetMapping("/halls/{id}") public CinemaHall hall(@PathVariable Long id) {
        return c.found(c.halls().findById(id));
    }
    @PostMapping("/halls") @ResponseStatus(HttpStatus.CREATED) public CinemaHall addHall(@RequestBody HallInput x) {
        return c.saveHall(null,x);
    }
    @PutMapping("/halls/{id}") public CinemaHall editHall(@PathVariable Long id,@RequestBody HallInput x) {
        return c.saveHall(id,x);
    }
    @GetMapping("/halls/{id}/seats") public List<Seat> seats(@PathVariable Long id) {
        hall(id);
        return c.seats().findByCinemaHallIdOrderByRowNumberAscSeatNumberAsc(id);
    }
    @PutMapping("/halls/{id}/layout") public List<Seat> layout(@PathVariable Long id,@RequestBody LayoutInput x) {
        return c.layout(id,x);
    }
    @PutMapping("/halls/{id}/seats/{seatId}") public Seat seat(@PathVariable Long id,@PathVariable Long seatId,@RequestBody SeatInput x) {
        return c.configureSeat(id,seatId,x);
    }
    @GetMapping("/screenings") public List<ScreeningView> screenings(@RequestParam(required=false) Long movieId,@RequestParam(defaultValue="false") boolean upcoming) {
        return c.screenings().findAll().stream().filter(s->s.getMovieEntity()!=null).filter(s->movieId==null||s.getMovieEntity().getId().equals(movieId)).filter(s->!upcoming||(!s.isCancelled()&&s.getMovieEntity().isActive()&&s.getCinemaHall().isActive()&&s.getStartsAt().isAfter(clock.instant().plusSeconds(900)))).sorted(Comparator.comparing(Screening::getStartsAt)).map(c::view).toList();
    }
    @GetMapping("/screenings/{id}") public ScreeningView screening(@PathVariable Long id) {
        return c.view(c.found(c.screenings().findById(id)));
    }
    @PostMapping("/screenings") @ResponseStatus(HttpStatus.CREATED) public ScreeningView addScreening(@RequestBody ScreeningInput x) {
        return c.view(c.saveScreening(null,x));
    }
    @PutMapping("/screenings/{id}") public ScreeningView editScreening(@PathVariable Long id,@RequestBody ScreeningInput x) {
        return c.view(c.saveScreening(id,x));
    }
    @PostMapping("/screenings/{id}/cancel") public ScreeningView cancel(@PathVariable Long id) {
        return c.view(c.cancelScreening(id));
    }
}
