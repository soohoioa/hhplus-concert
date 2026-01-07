package kr.hhplus.be.server.reservation.application.dto;

import lombok.Value;

@Value
public class HoldSeatCommand {
    Long userId;
    Long scheduleId;
    Integer seatNo;
}
