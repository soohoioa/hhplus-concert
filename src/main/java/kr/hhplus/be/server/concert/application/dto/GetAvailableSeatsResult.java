package kr.hhplus.be.server.concert.application.dto;

import lombok.Value;

import java.util.List;

@Value
public class GetAvailableSeatsResult {
    Long scheduleId;
    List<Integer> availableSeatNos;
}