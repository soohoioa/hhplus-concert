package kr.hhplus.be.server.concert.controller;

import kr.hhplus.be.server.concert.application.dto.CreateScheduleCommand;
import kr.hhplus.be.server.concert.application.dto.CreateScheduleResult;
import kr.hhplus.be.server.concert.application.service.CreateScheduleUseCase;
import kr.hhplus.be.server.concert.controller.dto.ScheduleCreateRequest;
import kr.hhplus.be.server.concert.controller.dto.ScheduleCreateResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/concert-schedules")
public class ConcertScheduleCommandController {

    private final CreateScheduleUseCase createScheduleUseCase;

    @PostMapping
    public ScheduleCreateResponse create(@RequestBody ScheduleCreateRequest req) {
        CreateScheduleResult result = createScheduleUseCase.create(
                new CreateScheduleCommand(req.getConcertId(), req.getStartAt())
        );
        return new ScheduleCreateResponse(result.getScheduleId(), result.getConcertId(), result.getStartAt());
    }
}
