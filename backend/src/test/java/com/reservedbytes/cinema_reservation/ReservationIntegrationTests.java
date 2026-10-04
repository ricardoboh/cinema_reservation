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
    @org.springframework.test.context.bean.override.mockito.MockitoSpyBean
    com.reservedbytes.cinema_reservation.service.NotificationService notifications;

    private User user;
    private Screening screening;
    private Seat first;
    private Seat second;

    @BeforeEach
    void setUp() {
        org.mockito.Mockito.clearInvocations(notifications);
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
    void createsDraftEvenWhenOneSeatIsConfirmed() throws Exception {
        seedReservation(screening, ReservationStatus.CONFIRMED);
        assertThat(post(request(user.getId(), screening.getId(), List.of(first.getId(), second.getId()))).statusCode()).isEqualTo(201);
        assertThat(reservations.count()).isEqualTo(2);
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
        // Fixture only: seed state directly to isolate the behavior under test.
        jdbc.update("update reservation set status = ? where id = ?", status.name(), saved.getId());
    }

    private long draft() throws Exception {
        return mapper.readTree(post(request(user.getId(), screening.getId(), List.of(first.getId()))).body()).get("id").asLong();
    }

    @Test
    void notificationsAreInvokedForCommittedTransitionsOnly() throws Exception {
        long id = draft();
        org.mockito.Mockito.verifyNoInteractions(notifications);
        assertThat(call("/reservations/" + id + "/confirm", user.getId()).statusCode()).isEqualTo(200);
        assertThat(call("/reservations/" + id + "/confirm", user.getId()).statusCode()).isEqualTo(409);
        assertThat(call("/reservations/" + id + "/cancel", user.getId()).statusCode()).isEqualTo(200);
        assertThat(call("/reservations/" + id + "/cancel", user.getId()).statusCode()).isEqualTo(409);
        org.mockito.Mockito.verify(notifications).notifyReservationChanged(org.mockito.ArgumentMatchers.argThat(r -> r.id() == id && r.status() == ReservationStatus.CONFIRMED));
        org.mockito.Mockito.verify(notifications).notifyReservationChanged(org.mockito.ArgumentMatchers.argThat(r -> r.id() == id && r.status() == ReservationStatus.CANCELLED));
        org.mockito.Mockito.verifyNoMoreInteractions(notifications);
    }

    private HttpResponse<String> call(String path, Long caller) throws Exception {
        var builder = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path));
        if (caller == null) builder.GET();
        else builder.header("X-User-Id", caller.toString()).POST(HttpRequest.BodyPublishers.noBody());
        try (var client = HttpClient.newHttpClient()) {
            return client.send(builder.build(), HttpResponse.BodyHandlers.ofString());
        }
    }

    private String availability() throws Exception {
        var response = call("/screenings/" + screening.getId() + "/availability", null);
        assertThat(response.statusCode()).isEqualTo(200);
        return mapper.readTree(response.body()).get(0).get("availability").asText();
    }

    @Test
    void availabilityTracksOnlyConfirmedAndCancellationPreservesHistory() throws Exception {
        long id = draft();
        long competing = draft();
        assertThat(availability()).isEqualTo("AVAILABLE");
        assertThat(call("/screenings/999999/availability", null).statusCode()).isEqualTo(404);
        var confirmed = call("/reservations/" + id + "/confirm", user.getId());
        assertThat(confirmed.statusCode()).isEqualTo(200);
        assertThat(mapper.readTree(confirmed.body()).get("status").asText()).isEqualTo("CONFIRMED");
        assertThat(availability()).isEqualTo("UNAVAILABLE");
        long alreadyAllocatedDraft = draft();
        assertThat(call("/reservations/" + competing + "/confirm", user.getId()).statusCode()).isEqualTo(409);
        assertThat(reservations.findById(competing).orElseThrow().getStatus()).isEqualTo(ReservationStatus.DRAFT);
        assertThat(call("/reservations/" + id + "/confirm", user.getId()).statusCode()).isEqualTo(409);
        assertThat(reservations.findById(id).orElseThrow().getStatus()).isEqualTo(ReservationStatus.CONFIRMED);
        var cancelled = call("/reservations/" + id + "/cancel", user.getId());
        assertThat(cancelled.statusCode()).isEqualTo(200);
        assertThat(mapper.readTree(cancelled.body()).get("status").asText()).isEqualTo("CANCELLED");
        assertThat(reservations.findById(id).orElseThrow().getCancelledAt()).isEqualTo(NOW);
        assertThat(availability()).isEqualTo("AVAILABLE");
        assertThat(call("/reservations/" + id + "/cancel", user.getId()).statusCode()).isEqualTo(409);
        assertThat(call("/reservations/" + id + "/confirm", user.getId()).statusCode()).isEqualTo(409);
        assertThat(reservations.findById(id).orElseThrow().getStatus()).isEqualTo(ReservationStatus.CANCELLED);
        assertThat(call("/reservations/" + alreadyAllocatedDraft + "/confirm", user.getId()).statusCode()).isEqualTo(200);
    }

    @Test
    void rejectsNonOwnersAndMissingReservationsWithoutChangingState() throws Exception {
        long id = draft();
        var other = users.save(new User());
        for (String operation : List.of("confirm", "cancel")) {
            assertThat(call("/reservations/" + id + "/" + operation, other.getId()).statusCode()).isEqualTo(403);
            assertThat(call("/reservations/999999/" + operation, user.getId()).statusCode()).isEqualTo(404);
            assertThat(reservations.findById(id).orElseThrow().getStatus()).isEqualTo(ReservationStatus.DRAFT);
        }
        assertThat(call("/reservations/" + id + "/cancel", user.getId()).statusCode()).isEqualTo(200);
    }

    @ParameterizedTest
    @ValueSource(longs = {1, 0, -1})
    void confirmAndCancelRespectStartBoundary(long secondsUntilStart) throws Exception {
        var target = screenings.save(new Screening("Boundary", "Hall A", NOW.plusSeconds(secondsUntilStart)));
        for (var initial : List.of(ReservationStatus.DRAFT, ReservationStatus.CONFIRMED)) {
            var saved = reservations.save(new Reservation(user, target, Set.of(second), NOW.minusSeconds(3600)));
            jdbc.update("update reservation set status = ? where id = ?", initial.name(), saved.getId());
            int expected = secondsUntilStart > 0 ? 200 : 409;
            assertThat(call("/reservations/" + saved.getId() + "/cancel", user.getId()).statusCode()).isEqualTo(expected);
            assertThat(reservations.findById(saved.getId()).orElseThrow().getStatus())
                .isEqualTo(expected == 200 ? ReservationStatus.CANCELLED : initial);
        }
        var saved = reservations.save(new Reservation(user, target, Set.of(first), NOW.minusSeconds(3600)));
        assertThat(call("/reservations/" + saved.getId() + "/confirm", user.getId()).statusCode()).isEqualTo(secondsUntilStart > 0 ? 200 : 409);
        assertThat(reservations.findById(saved.getId()).orElseThrow().getStatus())
            .isEqualTo(secondsUntilStart > 0 ? ReservationStatus.CONFIRMED : ReservationStatus.DRAFT);
    }

    @org.junit.jupiter.api.RepeatedTest(5)
    void concurrentConflictingConfirmationsHaveOneWinner() throws Exception {
        long a = draft(), b = draft();
        var results = race("/reservations/" + a + "/confirm", "/reservations/" + b + "/confirm");
        assertThat(results).containsExactlyInAnyOrder(200, 409);
        assertThat(reservations.findAll().stream().filter(r -> r.getStatus() == ReservationStatus.CONFIRMED).count()).isEqualTo(1);
    }

    @org.junit.jupiter.api.RepeatedTest(5)
    void concurrentConfirmCancelLeavesCancelledAndNeverResurrects() throws Exception {
        long id = draft();
        var results = race("/reservations/" + id + "/confirm", "/reservations/" + id + "/cancel");
        assertThat(results.get(0)).isIn(200, 409);
        assertThat(results.get(1)).isEqualTo(200);
        assertThat(reservations.findById(id).orElseThrow().getStatus()).isEqualTo(ReservationStatus.CANCELLED);
        assertThat(availability()).isEqualTo("AVAILABLE");
    }

    private List<Integer> race(String a, String b) throws Exception {
        var gate = new java.util.concurrent.CyclicBarrier(2);
        try (var executor = java.util.concurrent.Executors.newFixedThreadPool(2)) {
            var firstCall = executor.submit(() -> { gate.await(10, java.util.concurrent.TimeUnit.SECONDS); return call(a, user.getId()).statusCode(); });
            var secondCall = executor.submit(() -> { gate.await(10, java.util.concurrent.TimeUnit.SECONDS); return call(b, user.getId()).statusCode(); });
            return List.of(firstCall.get(20, java.util.concurrent.TimeUnit.SECONDS), secondCall.get(20, java.util.concurrent.TimeUnit.SECONDS));
        }
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

