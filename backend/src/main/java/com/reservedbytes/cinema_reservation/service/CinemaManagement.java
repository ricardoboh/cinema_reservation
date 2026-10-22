package com.reservedbytes.cinema_reservation.service;
import com.reservedbytes.cinema_reservation.model.*;
import com.reservedbytes.cinema_reservation.repository.*;
import com.reservedbytes.cinema_reservation.exception.ReservationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import java.time.*;
import java.util.*;
@Service @Transactional public class CinemaManagement  {
    private final MovieRepository movies;
    private final CinemaHallRepository halls;
    private final ScreeningRepository screenings;
    private final SeatRepository seats;
    public MovieRepository movies() {
        return movies;
    }
    public CinemaHallRepository halls() {
        return halls;
    }
    public ScreeningRepository screenings() {
        return screenings;
    }
    public SeatRepository seats() {
        return seats;
    }
    private final ReservationRepository reservations;
    private final Clock clock;
    public CinemaManagement(MovieRepository m,CinemaHallRepository h,ScreeningRepository s,SeatRepository t,ReservationRepository r,Clock c) {
        movies=m;
        halls=h;
        screenings=s;
        seats=t;
        reservations=r;
        clock=c;
    }
    public static void check(boolean ok,String msg) {
        if(!ok)throw new ReservationException(HttpStatus.CONFLICT,msg);
    }
    public static void input(boolean ok,String msg) {
        if(!ok)throw new ReservationException(HttpStatus.BAD_REQUEST,msg);
    }
    public <T>T found(Optional<T>x) {
        return x.orElseThrow(()->new ReservationException(HttpStatus.NOT_FOUND,"Record not found."));
    }
    public record MovieInput(String title,String description,String genre,int durationMinutes,int releaseYear,String posterUrl,boolean active) {
    }
    public Movie saveMovie(Long id,MovieInput x) {
        input(x.title()!=null&&!x.title().isBlank()&&x.title().length()<=255,"Title required (max 255).");
        input(x.durationMinutes()>0&&x.durationMinutes()<=600&&x.releaseYear()>=1888&&x.releaseYear()<=2200,"Invalid duration or year.");
        input(x.description()==null||x.description().length()<=5000,"Description too long.");
        input(x.genre()==null||x.genre().length()<=255,"Genre too long.");
        input(x.posterUrl()==null||x.posterUrl().isBlank()||(x.posterUrl().length()<=2000&&x.posterUrl().matches("https?://[^\\s]+")),"Poster must be an HTTP(S) URL.");
        Movie m=id==null?new Movie():found(movies.lockById(id));
        m.setTitle(x.title().trim());
        m.setDescription(x.description());
        m.setGenre(x.genre());
        m.setDurationMinutes(x.durationMinutes());
        m.setReleaseYear(x.releaseYear());
        m.setPosterUrl(x.posterUrl());
        m.setActive(x.active());
        return movies.save(m);
    }
    public void deleteMovie(Long id) {
        Movie m=found(movies.lockById(id));
        check(screenings.findAll().stream().noneMatch(s->s.getMovieEntity()!=null&&s.getMovieEntity().getId().equals(id)),"Movie has screenings; deactivate it instead.");
        movies.delete(m);
    }
    public record HallInput(String name,boolean active) {
    }
    public CinemaHall saveHall(Long id,HallInput x) {
        input(x.name()!=null&&!x.name().isBlank()&&x.name().length()<=255,"Hall name required (max 255).");
        CinemaHall h=id==null?new CinemaHall():found(halls.lockById(id));
        h.setName(x.name().trim());
        h.setActive(x.active());
        return halls.save(h);
    }
    public record LayoutInput(int rows,int seatsPerRow) {
    }
    public List<Seat> layout(Long id,LayoutInput x) {
        CinemaHall h=found(halls.lockById(id));
        input(x.rows()>0&&x.rows()<=30&&x.seatsPerRow()>0&&x.seatsPerRow()<=40,"Use 1?30 rows and 1?40 seats per row.");
        check(!reservations.existsByScreeningCinemaHallId(id),"Layout has reservation history and cannot be replaced.");
        seats.deleteAll(seats.findByCinemaHallIdOrderByRowNumberAscSeatNumberAsc(id));
        seats.flush();
        List<Seat> result=new ArrayList<>();
        for(int r=1;r<=x.rows();r++)for(int n=1;n<=x.seatsPerRow();n++)result.add(new Seat(h,r,n));
        return seats.saveAll(result);
    }
    public record SeatInput(Seat.SeatType seatType,boolean active) {
    }
    public Seat configureSeat(Long hallId,Long seatId,SeatInput x) {
        found(halls.lockById(hallId));
        Seat s=found(seats.findById(seatId));
        input(s.getCinemaHall()!=null&&s.getCinemaHall().getId().equals(hallId),"Seat belongs to another hall.");
        input(x.seatType()!=null,"Seat type required.");
        check(!reservations.existsBySeatsId(seatId),"Seat has reservation history; configuration locked.");
        s.configure(x.seatType(),x.active());
        return s;
    }
    public record ScreeningInput(Long movieId,Long hallId,Instant startsAt) {
    }
    public Screening saveScreening(Long id,ScreeningInput x) {
        input(x.movieId()!=null&&x.hallId()!=null&&x.startsAt()!=null,"Movie, hall and start required.");
        Movie m=found(movies.lockById(x.movieId()));
        CinemaHall h=found(halls.lockById(x.hallId()));
        Screening s=id==null?new Screening(m,h,x.startsAt()):found(screenings.findLockedById(id));
        check(!s.isCancelled(),"Cancelled screenings cannot be edited.");
        check(id==null||!reservations.existsByScreeningId(id),"Screening has reservation history; schedule locked.");
        input(x.startsAt().isAfter(clock.instant()),"Start must be in the future.");
        check(m.isActive()&&h.isActive(),"Movie and hall must be active.");
        check(!seats.findByCinemaHallIdOrderByRowNumberAscSeatNumberAsc(h.getId()).isEmpty(),"Configure hall seats first.");
        Instant end=x.startsAt().plusSeconds(m.getDurationMinutes()*60L);
        check(screenings.findAll().stream().noneMatch(a->!Objects.equals(a.getId(),id)&&!a.isCancelled()&&a.getCinemaHall()!=null&&a.getCinemaHall().getId().equals(h.getId())&&a.getStartsAt().isBefore(end)&&a.getEndsAt().isAfter(x.startsAt())),"Screenings overlap in this hall.");
        s.configure(m,h,x.startsAt());
        return screenings.save(s);
    }
    public Screening cancelScreening(Long id) {
        Screening s=found(screenings.findLockedById(id));
        check(!reservations.existsByScreeningId(id),"Screening has reservation history; cancellation restricted.");
        s.cancelScreening();
        return s;
    }
    public record ScreeningView(Long id,Movie movie,CinemaHall hall,Instant startsAt,Instant endsAt,boolean cancelled) {
    }
    public ScreeningView view(Screening s) {
        return new ScreeningView(s.getId(),s.getMovieEntity(),s.getCinemaHall(),s.getStartsAt(),s.getEndsAt(),s.isCancelled());
    }
}
