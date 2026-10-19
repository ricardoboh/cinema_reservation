package com.reservedbytes.cinema_reservation;

import com.reservedbytes.cinema_reservation.model.ReservationStatus;
import com.reservedbytes.cinema_reservation.service.FinitePendingExpirationPolicy;
import java.time.*;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class PendingExpirationPolicyTests {
    private final Instant requested = Instant.parse("2030-06-01T12:00:00Z");
    private final FinitePendingExpirationPolicy policy = new FinitePendingExpirationPolicy(Duration.ofSeconds(60));

    @Test void usesTimeoutWhenScreeningIsLater() {
        assertThat(policy.deadline(requested, requested.plusSeconds(3600))).isEqualTo(requested.plusSeconds(60));
    }
    @Test void capsDeadlineAtScreeningIncludingEquality() {
        assertThat(policy.deadline(requested, requested.plusSeconds(30))).isEqualTo(requested.plusSeconds(30));
        assertThat(policy.deadline(requested, requested.plusSeconds(60))).isEqualTo(requested.plusSeconds(60));
    }
    @Test void handlesHugeTimeoutWithoutInstantOverflow() {
        var huge = new FinitePendingExpirationPolicy(Duration.ofSeconds(Long.MAX_VALUE));
        assertThat(huge.deadline(requested, requested.plusSeconds(30))).isEqualTo(requested.plusSeconds(30));
    }
    @Test void rejectsNonPositiveOrMissingConfiguration() {
        for (var timeout : new Duration[] {null, Duration.ZERO, Duration.ofNanos(-1)}) {
            assertThatIllegalArgumentException().isThrownBy(() -> new FinitePendingExpirationPolicy(timeout));
        }
    }
    @Test void rejectsRequestAtOrAfterStart() {
        assertThatIllegalArgumentException().isThrownBy(() -> policy.deadline(requested, requested));
        assertThatIllegalArgumentException().isThrownBy(() -> policy.deadline(requested, requested.minusNanos(1)));
    }
    @Test void expiresExactlyAtDeadlineWithNanosecondPrecision() {
        var deadline = requested.plusSeconds(60);
        assertThat(policy.isDue(ReservationStatus.PENDING_APPROVAL, deadline, deadline.minusNanos(1))).isFalse();
        assertThat(policy.isDue(ReservationStatus.PENDING_APPROVAL, deadline, deadline)).isTrue();
        assertThat(policy.isDue(ReservationStatus.PENDING_APPROVAL, deadline, deadline.plusNanos(1))).isTrue();
    }
    @Test void neverExpiresNonPendingStates() {
        for (var status : ReservationStatus.values()) {
            if (status != ReservationStatus.PENDING_APPROVAL) {
                assertThat(policy.isDue(status, null, requested)).isFalse();
            }
        }
    }
    @Test void failsFastOnMissingRequiredInputs() {
        assertThatNullPointerException().isThrownBy(() -> policy.deadline(null, requested));
        assertThatNullPointerException().isThrownBy(() -> policy.deadline(requested, null));
        assertThatNullPointerException().isThrownBy(() -> policy.isDue(null, null, requested));
        assertThatNullPointerException().isThrownBy(() -> policy.isDue(ReservationStatus.PENDING_APPROVAL, null, requested));
        assertThatNullPointerException().isThrownBy(() -> policy.isDue(ReservationStatus.DRAFT, null, null));
    }
}
