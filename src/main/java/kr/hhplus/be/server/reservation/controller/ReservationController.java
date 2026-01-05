package kr.hhplus.be.server.reservation.controller;

import kr.hhplus.be.server.reservation.dto.SeatHoldRequest;
import kr.hhplus.be.server.reservation.dto.SeatHoldResponse;
import kr.hhplus.be.server.reservation.service.SeatHoldService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/reservations")
public class ReservationController {

    private final SeatHoldService seatHoldService;

    @PostMapping("/hold")
    public SeatHoldResponse hold(@RequestBody SeatHoldRequest req) {
        return seatHoldService.holdSeat(req);
    }

}
