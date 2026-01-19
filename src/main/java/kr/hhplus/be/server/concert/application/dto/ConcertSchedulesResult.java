package kr.hhplus.be.server.concert.application.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

import java.util.List;

@Getter
public class ConcertSchedulesResult {

    private Long concertId;
    private List<ScheduleItem> schedules;

    protected ConcertSchedulesResult() {
    }

    @JsonCreator
    public ConcertSchedulesResult(
            @JsonProperty("concertId") Long concertId,
            @JsonProperty("schedules") List<ScheduleItem> schedules
    ) {
        this.concertId = concertId;
        this.schedules = schedules;
    }
}
