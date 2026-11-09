package com.reservedbytes.cinema_reservation.service;

import com.reservedbytes.cinema_reservation.dto.CinemaRequests.*;
import com.reservedbytes.cinema_reservation.dto.CinemaViews.*;
import com.reservedbytes.cinema_reservation.exception.ReservationException;
import com.reservedbytes.cinema_reservation.model.*;
import com.reservedbytes.cinema_reservation.repository.*;
import java.time.Clock;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** PROJECT_FRAME administrator: catalog, halls/layout/VIP and immutable screenings. */
@Service
@Transactional
public class CinemaManagement {
    private final MovieRepository movies;
    private final CinemaHallRepository halls;
    private final ScreeningRepository screenings;
    private final SeatRepository seats;
    private final Clock clock;

    public CinemaManagement(MovieRepository movies, CinemaHallRepository halls,
            ScreeningRepository screenings, SeatRepository seats, Clock clock) {
        this.movies = movies;
        this.halls = halls;
        this.screenings = screenings;
        this.seats = seats;
        this.clock = clock;
    }

    private static <T> T found(Optional<T> result) {
        return result.orElseThrow(() -> new ReservationException(HttpStatus.NOT_FOUND, "Record not found."));
    }
    private static String name(String value) {
        if (value == null || value.isBlank()) {
            throw new ReservationException(HttpStatus.BAD_REQUEST, "A name is required.");
        }
        return value.trim();
    }
    private static MovieView view(Movie movie) { return new MovieView(movie.getId(), movie.getTitle()); }
    private static HallView view(CinemaHall hall) { return new HallView(hall.getId(), hall.getName()); }
    private static SeatView view(Seat seat) {
        return new SeatView(seat.getId(), seat.getRowNumber(), seat.getSeatNumber(), seat.getSeatType().name());
    }
    private static ScreeningView view(Screening screening) {
        return new ScreeningView(screening.getId(), view(screening.getMovieEntity()),
            view(screening.getCinemaHall()), screening.getStartsAt());
    }

    @Transactional(readOnly = true)
    public List<MovieView> listMovies() { return movies.findAll().stream().map(CinemaManagement::view).toList(); }
    @Transactional(readOnly = true)
    public MovieView getMovie(Long id) { return view(found(movies.findById(id))); }
    public MovieView saveMovie(Long id, MovieInput input) {
        String title = name(input.title());
        Movie movie = id == null ? new Movie(title) : found(movies.lockById(id));
        movie.rename(title);
        return view(movies.save(movie));
    }
    public void deleteMovie(Long id) {
        Movie movie = found(movies.lockById(id));
        if (screenings.existsByMovieEntityId(id)) {
            throw new ReservationException(HttpStatus.CONFLICT, "Movie is referenced by a screening.");
        }
        movies.delete(movie);
    }
    @Transactional(readOnly = true)
    public List<HallView> listHalls() { return halls.findAll().stream().map(CinemaManagement::view).toList(); }
    @Transactional(readOnly = true)
    public HallView getHall(Long id) { return view(found(halls.findById(id))); }
    public HallView saveHall(Long id, HallInput input) {
        String hallName = name(input.name());
        CinemaHall hall = id == null ? new CinemaHall(hallName) : found(halls.lockById(id));
        hall.rename(hallName);
        return view(halls.save(hall));
    }
    @Transactional(readOnly = true)
    public List<SeatView> listSeats(Long hallId) {
        found(halls.findById(hallId));
        return seats.findByCinemaHallIdOrderByRowNumberAscSeatNumberAsc(hallId).stream()
            .map(CinemaManagement::view).toList();
    }
    private void requireUnpublishedLayout(Long hallId) {
        if (screenings.existsByCinemaHallId(hallId)) {
            throw new ReservationException(HttpStatus.CONFLICT,
                "A published screening fixes its hall layout and seat classification.");
        }
    }
    public List<SeatView> layout(Long hallId, LayoutInput input) {
        CinemaHall hall = found(halls.lockById(hallId));
        if (input.rows() <= 0 || input.seatsPerRow() <= 0) {
            throw new ReservationException(HttpStatus.BAD_REQUEST, "Rows and seats per row must be positive.");
        }
        requireUnpublishedLayout(hallId);
        seats.deleteAll(seats.findByCinemaHallIdOrderByRowNumberAscSeatNumberAsc(hallId));
        seats.flush();
        List<Seat> layout = new ArrayList<>();
        for (int row = 1; row <= input.rows(); row++) {
            for (int seat = 1; seat <= input.seatsPerRow(); seat++) layout.add(new Seat(hall, row, seat));
        }
        return seats.saveAll(layout).stream().map(CinemaManagement::view).toList();
    }
    public SeatView configureSeat(Long hallId, Long seatId, SeatInput input) {
        found(halls.lockById(hallId));
        Seat seat = found(seats.findById(seatId));
        if (seat.getCinemaHall() == null || !seat.getCinemaHall().getId().equals(hallId)) {
            throw new ReservationException(HttpStatus.BAD_REQUEST, "Seat belongs to another hall.");
        }
        Seat.SeatType type;
        try { type = Seat.SeatType.valueOf(input.seatType() == null ? "" : input.seatType()); }
        catch (IllegalArgumentException exception) {
            throw new ReservationException(HttpStatus.BAD_REQUEST, "Seat type must be STANDARD or VIP.");
        }
        requireUnpublishedLayout(hallId);
        seat.classify(type);
        return view(seat);
    }
    @Transactional(readOnly = true)
    public List<ScreeningView> listScreenings(Long movieId, boolean upcoming) {
        var cutoff = clock.instant().plusSeconds(900);
        return screenings.findAll().stream()
            .filter(screening -> screening.getMovieEntity() != null)
            .filter(screening -> movieId == null || screening.getMovieEntity().getId().equals(movieId))
            .filter(screening -> !upcoming || !screening.getStartsAt().isBefore(cutoff))
            .sorted(Comparator.comparing(Screening::getStartsAt))
            .map(CinemaManagement::view).toList();
    }
    @Transactional(readOnly = true)
    public ScreeningView getScreening(Long id) { return view(found(screenings.findById(id))); }
    public ScreeningView createScreening(ScreeningInput input) {
        if (input.movieId() == null || input.hallId() == null || input.startsAt() == null) {
            throw new ReservationException(HttpStatus.BAD_REQUEST, "Movie, hall and UTC start are required.");
        }
        Movie movie = found(movies.lockById(input.movieId()));
        CinemaHall hall = found(halls.lockById(input.hallId()));
        if (seats.findByCinemaHallIdOrderByRowNumberAscSeatNumberAsc(hall.getId()).isEmpty()) {
            throw new ReservationException(HttpStatus.BAD_REQUEST, "Configure the screening hall layout first.");
        }
        return view(screenings.save(new Screening(movie, hall, input.startsAt())));
    }
}
