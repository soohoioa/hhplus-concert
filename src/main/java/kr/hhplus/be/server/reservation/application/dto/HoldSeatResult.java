package kr.hhplus.be.server.reservation.application.dto;

import lombok.Value;

import java.time.LocalDateTime;

@Value
public class HoldSeatResult {
    Long scheduleId;
    Integer seatNo;
    LocalDateTime holdExpiresAt;
}
