package kr.hhplus.be.server.point.controller;

import kr.hhplus.be.server.point.dto.PointBalanceResponse;
import kr.hhplus.be.server.point.dto.PointChargeRequest;
import kr.hhplus.be.server.point.dto.PointChargeResponse;
import kr.hhplus.be.server.point.service.PointService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/points")
public class PointController {

    private final PointService pointService;

    @PostMapping("/charge")
    public PointChargeResponse charge(@RequestBody PointChargeRequest req) {
        return pointService.charge(req);
    }

    @GetMapping("/{userId}")
    public PointBalanceResponse get(@PathVariable Long userId) {
        return pointService.getBalance(userId);
    }

}
