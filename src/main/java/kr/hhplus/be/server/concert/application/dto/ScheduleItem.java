package kr.hhplus.be.server.concert.application.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class ScheduleItem {
    private final Long id;
    private final LocalDateTime startAt;

    @JsonCreator
    public ScheduleItem(
            @JsonProperty("id") Long id,
            @JsonProperty("startAt") LocalDateTime startAt
    ) {
        this.id = id;
        this.startAt = startAt;
    }
}

