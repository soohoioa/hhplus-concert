package kr.hhplus.be.server.concert.controller;

import kr.hhplus.be.server.concert.dto.AvailableSeatsResponse;
import kr.hhplus.be.server.concert.dto.ScheduleResponse;
import kr.hhplus.be.server.concert.service.ConcertQueryService;
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

    private final ConcertQueryService concertQueryService;

    @GetMapping("/{concertId}/schedules")
    public List<ScheduleResponse> getSchedules(@PathVariable Long concertId) {
        return concertQueryService.getSchedules(concertId);
    }

    @GetMapping("/{concertId}/schedules/{scheduleId}/seats")
    public AvailableSeatsResponse getAvailableSeats(
            @PathVariable Long concertId,
            @PathVariable Long scheduleId
    ) {
        return concertQueryService.getAvailableSeats(concertId, scheduleId);
    }

}
