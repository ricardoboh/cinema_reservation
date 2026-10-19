package com.reservedbytes.cinema_reservation;

import com.reservedbytes.cinema_reservation.dto.ReservationResponse;
import com.reservedbytes.cinema_reservation.model.*;
import com.reservedbytes.cinema_reservation.repository.*;
import com.reservedbytes.cinema_reservation.service.*;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class ExpirationServiceTests {
    private final Instant deadline = Instant.parse("2030-06-01T12:01:00Z");
    private final ReservationRepository reservations = mock(ReservationRepository.class);
    private final ScreeningRepository screenings = mock(ScreeningRepository.class);
    private final ApplicationEventPublisher events = mock(ApplicationEventPublisher.class);
    private final ExpirationService service = new ExpirationService(reservations, screenings,
        Clock.fixed(deadline.plusSeconds(30), ZoneOffset.UTC), events,
        new FinitePendingExpirationPolicy(Duration.ofSeconds(60)));

    private Reservation pending() {
        var r = new Reservation(new User(), new Screening("Movie", "A", deadline.plusSeconds(3600)),
                Set.of(), deadline.minusSeconds(60));
        r.requestApproval(deadline.minusSeconds(60), deadline, Set.of(3L), 3L);
        return r;
    }
    @Test void dueTransitionKeepsEffectiveDeadlineAndPublishesOnce() {
        var r = pending();
        assertThat(service.expireIfDueLocked(r, deadline.plusSeconds(30))).isTrue();
        assertThat(r.getStatus()).isEqualTo(ReservationStatus.EXPIRED);
        assertThat(r.getExpiredAt()).isEqualTo(deadline);
        assertThat(service.expireIfDueLocked(r, deadline.plusSeconds(31))).isFalse();
        var event = ArgumentCaptor.forClass(Object.class);
        verify(events).publishEvent(event.capture());
        assertThat(((ReservationResponse) event.getValue()).status()).isEqualTo(ReservationStatus.EXPIRED);
        verifyNoMoreInteractions(events);
    }
    @Test void livePendingDoesNotMutateOrNotify() {
        var r = pending();
        assertThat(service.expireIfDueLocked(r, deadline.minusNanos(1))).isFalse();
        assertThat(r.getStatus()).isEqualTo(ReservationStatus.PENDING_APPROVAL);
        assertThat(r.getExpiredAt()).isNull();
        verifyNoInteractions(events);
    }
    @Test void batchObtainsLockBeforeLoadingPendingAndUsesClock() {
        when(screenings.findLockedById(1L)).thenReturn(Optional.of(new Screening("Movie", "A", deadline.plusSeconds(3600))));
        var due = pending();
        var live = pending();
        live.requestApproval(deadline, deadline.plusSeconds(60), Set.of(3L), 3L);
        when(reservations.findByScreeningIdAndStatus(1L, ReservationStatus.PENDING_APPROVAL)).thenReturn(List.of(due, live));
        service.expireScreening(1L);
        var order = inOrder(screenings, reservations, events);
        order.verify(screenings).findLockedById(1L);
        order.verify(reservations).findByScreeningIdAndStatus(1L, ReservationStatus.PENDING_APPROVAL);
        order.verify(events).publishEvent(any(Object.class));
        assertThat(due.getStatus()).isEqualTo(ReservationStatus.EXPIRED);
        assertThat(live.getStatus()).isEqualTo(ReservationStatus.PENDING_APPROVAL);
    }
    @Test void missingScreeningIsNoOp() {
        when(screenings.findLockedById(999L)).thenReturn(Optional.empty());
        service.expireScreening(999L);
        verifyNoInteractions(reservations, events);
    }
}
