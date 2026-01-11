package kr.hhplus.be.server.concert.application.dto;

import lombok.Value;

import java.time.LocalDateTime;

@Value
public class CreateScheduleCommand {
    Long concertId;
    LocalDateTime startAt;
}
