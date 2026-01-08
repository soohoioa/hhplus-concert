package kr.hhplus.be.server.reservation.dto;

import lombok.Value;

import java.time.LocalDateTime;

@Value
public class SeatHoldResponse {
    Long scheduleId;
    Integer seatNo;
    LocalDateTime holdExpiresAt;
}
