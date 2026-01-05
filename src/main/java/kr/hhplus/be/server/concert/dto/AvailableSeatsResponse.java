package kr.hhplus.be.server.concert.dto;

import lombok.Value;

import java.util.List;

@Value
public class AvailableSeatsResponse {
    Long scheduleId;
    List<Integer> availableSeatNos;
}
