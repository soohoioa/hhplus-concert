package kr.hhplus.be.server.reservation.controller;

import kr.hhplus.be.server.queue.application.QueueService;
import kr.hhplus.be.server.reservation.application.service.HoldSeatUseCase;
import kr.hhplus.be.server.reservation.application.dto.HoldSeatCommand;
import kr.hhplus.be.server.reservation.application.dto.HoldSeatResult;
import kr.hhplus.be.server.reservation.controller.dto.SeatHoldRequest;
import kr.hhplus.be.server.reservation.controller.dto.SeatHoldResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/reservations")
public class ReservationController {

    private final HoldSeatUseCase holdSeatUseCase;
    private final QueueService queueService; // 추가

    @PostMapping("/hold")
    public SeatHoldResponse hold(
            @RequestHeader("X-QUEUE-TOKEN") String queueToken, // 추가
            @RequestBody SeatHoldRequest req
    ) {
        queueService.validateReady(queueToken); // Gate

        HoldSeatResult result = holdSeatUseCase.hold(
                new HoldSeatCommand(req.getUserId(), req.getScheduleId(), req.getSeatNo())
        );

        return new SeatHoldResponse(
                result.getScheduleId(),
                result.getSeatNo(),
                result.getHoldExpiresAt()
        );
    }
}
