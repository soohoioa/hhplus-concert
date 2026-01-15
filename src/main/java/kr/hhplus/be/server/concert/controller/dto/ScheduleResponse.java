package kr.hhplus.be.server.concert.controller.dto;

import lombok.Value;

import java.time.LocalDateTime;

@Value
public class ScheduleResponse {
    Long scheduleId;
    LocalDateTime startAt;
}
