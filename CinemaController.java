package com.reservedbytes.cinema_reservation.controller;

import com.reservedbytes.cinema_reservation.dto.CinemaRequests.*;
import com.reservedbytes.cinema_reservation.dto.CinemaViews.*;
import com.reservedbytes.cinema_reservation.service.CinemaManagement;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
public class CinemaController {
    private final CinemaManagement service;
    public CinemaController(CinemaManagement service) { this.service = service; }

    @GetMapping("/movies")
    public List<MovieView> movies() { return service.listMovies(); }
    @GetMapping("/movies/{id}")
    public MovieView movie(@PathVariable Long id) { return service.getMovie(id); }
    @PostMapping("/movies") @ResponseStatus(HttpStatus.CREATED)
    public MovieView createMovie(@RequestBody MovieInput input) { return service.saveMovie(null, input); }
    @PutMapping("/movies/{id}")
    public MovieView updateMovie(@PathVariable Long id, @RequestBody MovieInput input) {
        return service.saveMovie(id, input);
    }
    @DeleteMapping("/movies/{id}") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteMovie(@PathVariable Long id) { service.deleteMovie(id); }
    @GetMapping("/halls")
    public List<HallView> halls() { return service.listHalls(); }
    @GetMapping("/halls/{id}")
    public HallView hall(@PathVariable Long id) { return service.getHall(id); }
    @PostMapping("/halls") @ResponseStatus(HttpStatus.CREATED)
    public HallView createHall(@RequestBody HallInput input) { return service.saveHall(null, input); }
    @PutMapping("/halls/{id}")
    public HallView updateHall(@PathVariable Long id, @RequestBody HallInput input) {
        return service.saveHall(id, input);
    }
    @GetMapping("/halls/{id}/seats")
    public List<SeatView> seats(@PathVariable Long id) { return service.listSeats(id); }
    @PutMapping("/halls/{id}/layout")
    public List<SeatView> layout(@PathVariable Long id, @RequestBody LayoutInput input) {
        return service.layout(id, input);
    }
    @PutMapping("/halls/{id}/seats/{seatId}")
    public SeatView classifySeat(@PathVariable Long id, @PathVariable Long seatId,
            @RequestBody SeatInput input) { return service.configureSeat(id, seatId, input); }
    @GetMapping("/screenings")
    public List<ScreeningView> screenings(@RequestParam(required = false) Long movieId,
            @RequestParam(defaultValue = "false") boolean upcoming) {
        return service.listScreenings(movieId, upcoming);
    }
    @GetMapping("/screenings/{id}")
    public ScreeningView screening(@PathVariable Long id) { return service.getScreening(id); }
    @PostMapping("/screenings") @ResponseStatus(HttpStatus.CREATED)
    public ScreeningView createScreening(@RequestBody ScreeningInput input) { return service.createScreening(input); }
}
