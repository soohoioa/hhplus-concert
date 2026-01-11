package kr.hhplus.be.server.concert.application.dto;

import lombok.Value;

import java.time.LocalDateTime;

@Value
public class CreateScheduleResult {
    Long scheduleId;
    Long concertId;
    LocalDateTime startAt;
}
