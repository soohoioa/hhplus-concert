package kr.hhplus.be.server.point.controller;

import kr.hhplus.be.server.point.application.dto.ChargePointCommand;
import kr.hhplus.be.server.point.application.dto.ChargePointResult;
import kr.hhplus.be.server.point.application.dto.GetPointBalanceQuery;
import kr.hhplus.be.server.point.application.dto.GetPointBalanceResult;
import kr.hhplus.be.server.point.application.service.ChargePointUseCase;
import kr.hhplus.be.server.point.application.service.GetPointBalanceUseCase;
import kr.hhplus.be.server.point.controller.dto.PointBalanceResponse;
import kr.hhplus.be.server.point.controller.dto.PointChargeRequest;
import kr.hhplus.be.server.point.controller.dto.PointChargeResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/points")
public class PointController {

    private final ChargePointUseCase chargePointUseCase;
    private final GetPointBalanceUseCase getPointBalanceUseCase;

    @PostMapping("/charge")
    public PointChargeResponse charge(@RequestBody PointChargeRequest pointChargeRequest) {
        ChargePointResult result = chargePointUseCase.charge(
                new ChargePointCommand(pointChargeRequest.getUserId(), pointChargeRequest.getAmount())
        );
        return new PointChargeResponse(result.getUserId(), result.getBalance());
    }

    @GetMapping("/{userId}")
    public PointBalanceResponse get(@PathVariable Long userId) {
        GetPointBalanceResult result = getPointBalanceUseCase.get(
                new GetPointBalanceQuery(userId)
        );
        return new PointBalanceResponse(result.getUserId(), result.getBalance());
    }

}
