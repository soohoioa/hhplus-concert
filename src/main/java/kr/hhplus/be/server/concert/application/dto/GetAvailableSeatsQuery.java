package kr.hhplus.be.server.concert.application.dto;

import lombok.Value;

@Value
public class GetAvailableSeatsQuery {
    Long concertId;
    Long scheduleId;
}
