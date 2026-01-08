package kr.hhplus.be.server.concert.dto;

import lombok.Value;

import java.time.LocalDateTime;

@Value
public class ScheduleCreateResponse {
    Long scheduleId;
    Long concertId;
    LocalDateTime startAt;
}
