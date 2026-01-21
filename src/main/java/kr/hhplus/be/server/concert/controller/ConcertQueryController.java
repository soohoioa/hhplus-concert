package kr.hhplus.be.server.concert.controller;

import kr.hhplus.be.server.concert.application.dto.*;
import kr.hhplus.be.server.concert.application.service.ConcertQueryUseCase;
import kr.hhplus.be.server.concert.controller.dto.AvailableSeatsResponse;
import kr.hhplus.be.server.concert.controller.dto.ScheduleResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/concerts")
public class ConcertQueryController {

    private final ConcertQueryUseCase concertQueryUseCase;

    @GetMapping("/{concertId}/schedules")
    public List<ScheduleResponse> getSchedules(@PathVariable Long concertId) {
        List<ScheduleItem> items = concertQueryUseCase.getSchedules(new GetSchedulesQuery(concertId));
        return items.stream()
                .map(i -> new ScheduleResponse(i.getId(), i.getStartAt()))
                .toList();
    }

    @GetMapping("/{concertId}/schedules/{scheduleId}/seats")
    public AvailableSeatsResponse getAvailableSeats(
            @PathVariable Long concertId,
            @PathVariable Long scheduleId
    ) {
        GetAvailableSeatsResult result = concertQueryUseCase.getAvailableSeats(
                new GetAvailableSeatsQuery(concertId, scheduleId)
        );
        return new AvailableSeatsResponse(result.getScheduleId(), result.getAvailableSeatNos());
    }

}
