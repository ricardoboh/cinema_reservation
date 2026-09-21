package com.reservedbytes.cinema_reservation;

import com.reservedbytes.cinema_reservation.model.*;
import com.reservedbytes.cinema_reservation.repository.*;
import java.net.URI;
import java.net.http.*;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.*;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(ReservationIntegrationTests.TimeConfiguration.class)
class ReservationIntegrationTests {
    private static final Instant NOW = Instant.parse("2030-06-01T12:00:00Z");

    @TestConfiguration
    static class TimeConfiguration {
        @Bean @Primary
        Clock testClock() {
            return Clock.fixed(NOW, ZoneOffset.UTC);
        }
    }

    @LocalServerPort int port;
    @Autowired UserRepository users;
    @Autowired ScreeningRepository screenings;
    @Autowired SeatRepository seats;
    @Autowired ReservationRepository reservations;
    @Autowired JdbcTemplate jdbc;
    @Autowired PlatformTransactionManager transactionManager;
    @Autowired ObjectMapper mapper;

    private User user;
    private Screening screening;
    private Seat first;
    private Seat second;

    @BeforeEach
    void setUp() {
        reservations.deleteAll();
        seats.deleteAll();
        screenings.deleteAll();
        users.deleteAll();
        user = users.save(new User());
        screening = screenings.save(new Screening("Movie", "Hall A", NOW.plusSeconds(3600)));
        first = seats.save(new Seat("Hall A", 1, 1));
        second = seats.save(new Seat("Hall A", 1, 2));
    }

    @Test
    void createsAndCommitsDraftWithAllSelectedSeats() throws Exception {
        var response = post(request(user.getId(), screening.getId(), List.of(first.getId(), second.getId())));
        assertThat(response.statusCode()).isEqualTo(201);
        var json = mapper.readTree(response.body());
        assertThat(json.size()).isEqualTo(1);
        long id = json.get("id").asLong();
        assertThat(id).isPositive();

        // The HTTP request has completed; read in a separate transaction to verify committed data.
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            var stored = reservations.findById(id).orElseThrow();
            assertThat(stored.getStatus()).isEqualTo(ReservationStatus.DRAFT);
            assertThat(stored.getUser().getId()).isEqualTo(user.getId());
            assertThat(stored.getScreening().getId()).isEqualTo(screening.getId());
            assertThat(stored.getSeats()).extracting(Seat::getId)
                .containsExactlyInAnyOrder(first.getId(), second.getId());
            assertThat(stored.getCreatedAt()).isEqualTo(NOW);
        });
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "{}", "null", "{", "{\"userId\":\"abc\"}",
        "{\"userId\":1,\"screeningId\":1,\"seatIds\":[]}",
        "{\"userId\":1,\"screeningId\":1,\"seatIds\":[null]}",
        "{\"userId\":1,\"screeningId\":1,\"seatIds\":[1,1]}",
        "{\"userId\":0,\"screeningId\":1,\"seatIds\":[1]}",
        "{\"userId\":1,\"screeningId\":-1,\"seatIds\":[1]}",
        "{\"userId\":1,\"screeningId\":1,\"seatIds\":[0]}"
    })
    void rejectsInvalidRequestsWithoutWriting(String body) throws Exception {
        assertThat(post(body).statusCode()).isEqualTo(400);
        assertThat(reservations.count()).isZero();
    }

    @Test
    void rejectsUnknownReferencesWithoutWriting() throws Exception {
        assertThat(post(request(Long.MAX_VALUE, screening.getId(), List.of(first.getId()))).statusCode()).isEqualTo(404);
        assertThat(post(request(user.getId(), Long.MAX_VALUE, List.of(first.getId()))).statusCode()).isEqualTo(404);
        assertThat(post(request(user.getId(), screening.getId(), List.of(first.getId(), Long.MAX_VALUE))).statusCode()).isEqualTo(404);
        assertThat(reservations.count()).isZero();
    }

    @Test
    void rejectsSeatInAnotherHall() throws Exception {
        var otherSeat = seats.save(new Seat("Hall B", 1, 1));
        assertThat(post(request(user.getId(), screening.getId(), List.of(otherSeat.getId()))).statusCode()).isEqualTo(400);
        assertThat(reservations.count()).isZero();
    }

    @Test
    void rejectsEntireRequestWhenOneSeatIsConfirmed() throws Exception {
        seedReservation(screening, ReservationStatus.CONFIRMED);
        assertThat(post(request(user.getId(), screening.getId(), List.of(first.getId(), second.getId()))).statusCode()).isEqualTo(409);
        assertThat(reservations.count()).isEqualTo(1);
    }

    @Test
    void draftsAndCancelledReservationsDoNotHoldSeats() throws Exception {
        seedReservation(screening, ReservationStatus.DRAFT);
        seedReservation(screening, ReservationStatus.CANCELLED);
        assertThat(post(request(user.getId(), screening.getId(), List.of(first.getId()))).statusCode()).isEqualTo(201);
        assertThat(reservations.count()).isEqualTo(3);
    }

    @Test
    void confirmedSeatAtAnotherScreeningDoesNotBlockCreation() throws Exception {
        var other = screenings.save(new Screening("Other movie", "Hall A", NOW.plusSeconds(7200)));
        seedReservation(other, ReservationStatus.CONFIRMED);
        assertThat(post(request(user.getId(), screening.getId(), List.of(first.getId()))).statusCode()).isEqualTo(201);
    }

    @Test
    void acceptsExactlyFifteenMinutesBeforeStart() throws Exception {
        var boundary = screenings.save(new Screening("Movie", "Hall A", NOW.plusSeconds(900)));
        assertThat(post(request(user.getId(), boundary.getId(), List.of(first.getId()))).statusCode()).isEqualTo(201);
        assertThat(reservations.count()).isEqualTo(1);
    }

    @ParameterizedTest
    @ValueSource(longs = {899, 0, -3600})
    void rejectsLessThanFifteenMinutesAndPastScreenings(long secondsUntilStart) throws Exception {
        var late = screenings.save(new Screening("Movie", "Hall A", NOW.plusSeconds(secondsUntilStart)));
        assertThat(post(request(user.getId(), late.getId(), List.of(first.getId()))).statusCode()).isEqualTo(409);
        assertThat(reservations.count()).isZero();
    }

    private void seedReservation(Screening target, ReservationStatus status) {
        var saved = reservations.save(new Reservation(user, target, Set.of(first), NOW.minusSeconds(60)));
        // Fixture only: confirmation/cancellation operations are intentionally outside this slice.
        jdbc.update("update reservation set status = ? where id = ?", status.name(), saved.getId());
    }

    private String request(Long userId, Long screeningId, List<Long> seatIds) throws Exception {
        return mapper.writeValueAsString(Map.of("userId", userId, "screeningId", screeningId, "seatIds", seatIds));
    }

    private HttpResponse<String> post(String body) throws Exception {
        var request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/reservations"))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(body)).build();
        try (var client = HttpClient.newHttpClient()) {
            return client.send(request, HttpResponse.BodyHandlers.ofString());
        }
    }
}

