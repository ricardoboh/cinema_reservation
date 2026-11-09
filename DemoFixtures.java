package com.reservedbytes.cinema_reservation.service;

import com.reservedbytes.cinema_reservation.dto.CinemaRequests.*;
import java.time.Clock;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Small C08 fixture: owner 1, other owner 2, approver 3; standard 1/2 and VIP 3/4. */
@Component
@Profile("dev")
public class DemoFixtures implements CommandLineRunner {
    private final CinemaManagement service;
    private final Clock clock;
    public DemoFixtures(CinemaManagement service, Clock clock) { this.service = service; this.clock = clock; }

    @Override @Transactional
    public void run(String... args) {
        var movie = service.saveMovie(null, new MovieInput("Demo movie"));
        var hall = service.saveHall(null, new HallInput("Hall A"));
        var seats = service.layout(hall.id(), new LayoutInput(1, 4));
        for (var seat : seats) if (seat.seatNumber() >= 3) {
            service.configureSeat(hall.id(), seat.id(), new SeatInput("VIP"));
        }
        var now = clock.instant();
        service.createScreening(new ScreeningInput(movie.id(), hall.id(), now.plusSeconds(86400)));
        service.createScreening(new ScreeningInput(movie.id(), hall.id(), now.plusSeconds(600)));
        service.createScreening(new ScreeningInput(movie.id(), hall.id(), now.minusSeconds(3600)));
    }
}
