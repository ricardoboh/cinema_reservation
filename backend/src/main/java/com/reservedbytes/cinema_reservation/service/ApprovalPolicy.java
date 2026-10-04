package com.reservedbytes.cinema_reservation.service;

import com.reservedbytes.cinema_reservation.model.Seat;
import org.springframework.stereotype.Component;

/** Deterministic C02 demo policy, not production policy or authentication. */
@Component
public class ApprovalPolicy {
    public static final Long DEMO_APPROVER_ID = 3L;

    public boolean requiresApproval(Seat seat) {
        return seat.getSeatNumber() == 3 || seat.getSeatNumber() == 4;
    }
}
