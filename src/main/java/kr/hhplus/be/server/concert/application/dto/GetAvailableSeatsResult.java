package kr.hhplus.be.server.concert.application.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

import java.util.List;

@Getter
public class GetAvailableSeatsResult {
    private final Long scheduleId;
    private final List<Integer> availableSeatNos;

    @JsonCreator
    public GetAvailableSeatsResult(
            @JsonProperty("scheduleId") Long scheduleId,
            @JsonProperty("availableSeatNos") List<Integer> availableSeatNos
    ) {
        this.scheduleId = scheduleId;
        this.availableSeatNos = availableSeatNos;
    }
}