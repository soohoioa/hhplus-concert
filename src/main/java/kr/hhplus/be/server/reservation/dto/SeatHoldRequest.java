package kr.hhplus.be.server.reservation.dto;

import lombok.Value;

@Value
public class SeatHoldRequest {
    Long userId;
    Long scheduleId;
    Integer seatNo;
}
