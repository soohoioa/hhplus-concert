package kr.hhplus.be.server.point.application.service;

import kr.hhplus.be.server.point.application.dto.ChargePointCommand;
import kr.hhplus.be.server.point.application.dto.ChargePointResult;

public interface ChargePointUseCase {
    ChargePointResult charge(ChargePointCommand command);
}
