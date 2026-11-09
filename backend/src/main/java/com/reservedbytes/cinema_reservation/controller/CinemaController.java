package com.reservedbytes.cinema_reservation.controller;
import com.reservedbytes.cinema_reservation.service.CinemaManagement;
import com.reservedbytes.cinema_reservation.service.CinemaManagement.*;
import com.reservedbytes.cinema_reservation.model.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import java.util.*;
@RestController public class CinemaController  {
    private final CinemaManagement c;
    public CinemaController(CinemaManagement c) {
        this.c=c;
    }
    @GetMapping("/movies") public List<Movie> movies() {
        return c.listMovies();
    }
    @GetMapping("/movies/{id}") public Movie movie(@PathVariable Long id) {
        return c.getMovie(id);
    }
    @PostMapping("/movies") @ResponseStatus(HttpStatus.CREATED) public Movie addMovie(@RequestBody MovieInput x) {
        return c.saveMovie(null,x);
    }
    @PutMapping("/movies/{id}") public Movie editMovie(@PathVariable Long id,@RequestBody MovieInput x) {
        return c.saveMovie(id,x);
    }
    @PostMapping("/movies/{id}/deactivate") public Movie deactivate(@PathVariable Long id) {
        return c.deactivateMovie(id);
    }
    @DeleteMapping("/movies/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) public void delete(@PathVariable Long id) {
        c.deleteMovie(id);
    }
    @GetMapping("/halls") public List<CinemaHall> halls() {
        return c.listHalls();
    }
    @GetMapping("/halls/{id}") public CinemaHall hall(@PathVariable Long id) {
        return c.getHall(id);
    }
    @PostMapping("/halls") @ResponseStatus(HttpStatus.CREATED) public CinemaHall addHall(@RequestBody HallInput x) {
        return c.saveHall(null,x);
    }
    @PutMapping("/halls/{id}") public CinemaHall editHall(@PathVariable Long id,@RequestBody HallInput x) {
        return c.saveHall(id,x);
    }
    @GetMapping("/halls/{id}/seats") public List<Seat> seats(@PathVariable Long id) {
        return c.listSeats(id);
    }
    @PutMapping("/halls/{id}/layout") public List<Seat> layout(@PathVariable Long id,@RequestBody LayoutInput x) {
        return c.layout(id,x);
    }
    @PutMapping("/halls/{id}/seats/{seatId}") public Seat seat(@PathVariable Long id,@PathVariable Long seatId,@RequestBody SeatInput x) {
        return c.configureSeat(id,seatId,x);
    }
    @GetMapping("/screenings") public List<ScreeningView> screenings(@RequestParam(required=false) Long movieId,@RequestParam(defaultValue="false") boolean upcoming) {
        return c.listScreenings(movieId,upcoming);
    }
    @GetMapping("/screenings/{id}") public ScreeningView screening(@PathVariable Long id) {
        return c.getScreening(id);
    }
    @PostMapping("/screenings") @ResponseStatus(HttpStatus.CREATED) public ScreeningView addScreening(@RequestBody ScreeningInput x) {
        return c.saveScreeningView(null,x);
    }
    @PutMapping("/screenings/{id}") public ScreeningView editScreening(@PathVariable Long id,@RequestBody ScreeningInput x) {
        return c.saveScreeningView(id,x);
    }
    @PostMapping("/screenings/{id}/cancel") public ScreeningView cancel(@PathVariable Long id) {
        return c.cancelScreeningView(id);
    }
}
