package com.reservedbytes.cinema_reservation;

import com.reservedbytes.cinema_reservation.dto.CinemaRequests.*;
import com.reservedbytes.cinema_reservation.dto.CreateReservationRequest;
import com.reservedbytes.cinema_reservation.dto.ApprovalRequest;
import com.reservedbytes.cinema_reservation.model.User;
import com.reservedbytes.cinema_reservation.model.ReservationStatus;
import com.reservedbytes.cinema_reservation.repository.UserRepository;
import com.reservedbytes.cinema_reservation.service.CinemaManagement;
import com.reservedbytes.cinema_reservation.service.ReservationService;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@Transactional
class CinemaManagementTests {
    @Autowired CinemaManagement cinema;
    @Autowired ReservationService reservations;
    @Autowired UserRepository users;

    @Test void catalogAndHallNamesCanChangeWithoutChangingIdentity() {
        var movie=cinema.saveMovie(null,new MovieInput("Movie"));
        var hall=cinema.saveHall(null,new HallInput("Hall"));
        assertThat(cinema.saveMovie(movie.id(),new MovieInput("Renamed")).id()).isEqualTo(movie.id());
        assertThat(cinema.saveHall(hall.id(),new HallInput("Renamed hall")).id()).isEqualTo(hall.id());
        assertThat(cinema.getMovie(movie.id()).title()).isEqualTo("Renamed");
        assertThat(cinema.getHall(hall.id()).name()).isEqualTo("Renamed hall");
    }
    @Test void publishingScreeningFreezesItsPhysicalLayoutAndVipClassification() {
        var movie=cinema.saveMovie(null,new MovieInput("Movie"));
        var hall=cinema.saveHall(null,new HallInput("Hall"));
        var seats=cinema.layout(hall.id(),new LayoutInput(2,3));
        cinema.configureSeat(hall.id(),seats.get(0).id(),new SeatInput("VIP"));
        var screening=cinema.createScreening(new ScreeningInput(movie.id(),hall.id(),Instant.now().plusSeconds(3600)));
        assertThat(screening.startsAt()).isNotNull();
        assertThatThrownBy(()->cinema.layout(hall.id(),new LayoutInput(1,2))).hasMessageContaining("published screening");
        assertThatThrownBy(()->cinema.configureSeat(hall.id(),seats.get(0).id(),new SeatInput("STANDARD"))).hasMessageContaining("published screening");
        assertThat(cinema.listSeats(hall.id())).hasSize(6);
        assertThat(cinema.listSeats(hall.id()).get(0).seatType()).isEqualTo("VIP");
        assertThatThrownBy(()->cinema.deleteMovie(movie.id())).hasMessageContaining("referenced");
    }
    @Test void samePhysicalSeatIsIndependentAcrossScreenings() {
        var movie=cinema.saveMovie(null,new MovieInput("Movie"));
        var hall=cinema.saveHall(null,new HallInput("Hall"));
        var seat=cinema.layout(hall.id(),new LayoutInput(1,1)).get(0);
        var start=Instant.now().plusSeconds(7200);
        var a=cinema.createScreening(new ScreeningInput(movie.id(),hall.id(),start));
        var b=cinema.createScreening(new ScreeningInput(movie.id(),hall.id(),start.plusSeconds(3600)));
        var owner=users.save(new User());
        var first=reservations.create(new CreateReservationRequest(owner.getId(),a.id(),List.of(seat.id())));
        assertThat(reservations.confirm(first.reservationId(),owner.getId()).status()).isEqualTo(ReservationStatus.CONFIRMED);
        assertThat(reservations.availability(b.id()).get(0).status()).isEqualTo("AVAILABLE");
        var second=reservations.create(new CreateReservationRequest(owner.getId(),b.id(),List.of(seat.id())));
        assertThat(reservations.confirm(second.reservationId(),owner.getId()).status()).isEqualTo(ReservationStatus.CONFIRMED);
    }
    @Test void approvalDependsOnVipTypeRatherThanSeatNumber() {
        var movie=cinema.saveMovie(null,new MovieInput("Movie"));
        var hall=cinema.saveHall(null,new HallInput("Hall"));
        var seats=cinema.layout(hall.id(),new LayoutInput(1,3));
        cinema.configureSeat(hall.id(),seats.get(0).id(),new SeatInput("VIP"));
        var screening=cinema.createScreening(new ScreeningInput(movie.id(),hall.id(),Instant.now().plusSeconds(7200)));
        var owner=users.save(new User());
        var standard=reservations.create(new CreateReservationRequest(owner.getId(),screening.id(),List.of(seats.get(2).id())));
        assertThat(reservations.confirm(standard.reservationId(),owner.getId()).status()).isEqualTo(ReservationStatus.CONFIRMED);
        var vip=reservations.create(new CreateReservationRequest(owner.getId(),screening.id(),List.of(seats.get(0).id())));
        assertThat(reservations.confirm(vip.reservationId(),owner.getId()).status()).isEqualTo(ReservationStatus.PENDING_APPROVAL);
        assertThat(reservations.approval(vip.reservationId(),3L,new ApprovalRequest("APPROVE")).status()).isEqualTo(ReservationStatus.CONFIRMED);
    }
    @Test void invalidPhysicalReferencesAndEmptyNamesAreRejected() {
        assertThatThrownBy(()->cinema.saveMovie(null,new MovieInput(" "))).hasMessageContaining("name");
        var movie=cinema.saveMovie(null,new MovieInput("Movie"));
        var hall=cinema.saveHall(null,new HallInput("Hall"));
        var other=cinema.saveHall(null,new HallInput("Other"));
        assertThatThrownBy(()->cinema.layout(hall.id(),new LayoutInput(0,2))).hasMessageContaining("positive");
        var seat=cinema.layout(hall.id(),new LayoutInput(1,1)).get(0);
        assertThatThrownBy(()->cinema.configureSeat(other.id(),seat.id(),new SeatInput("VIP"))).hasMessageContaining("another hall");
        assertThatThrownBy(()->cinema.configureSeat(hall.id(),seat.id(),new SeatInput("OTHER"))).hasMessageContaining("STANDARD or VIP");
        assertThatThrownBy(()->cinema.createScreening(new ScreeningInput(movie.id(),other.id(),Instant.now()))).hasMessageContaining("layout first");
    }
}
