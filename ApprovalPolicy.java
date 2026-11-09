package com.reservedbytes.cinema_reservation.service;

import com.reservedbytes.cinema_reservation.model.Seat;
import org.springframework.stereotype.Component;

/** C08 demo authority; VIP classification implements BR-07. */
@Component
public class ApprovalPolicy {
    public static final Long DEMO_APPROVER_ID = 3L;

    public boolean requiresApproval(Seat seat) {
        return seat.getSeatType() == Seat.SeatType.VIP;
    }
}
