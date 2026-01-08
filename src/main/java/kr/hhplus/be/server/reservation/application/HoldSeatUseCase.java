package kr.hhplus.be.server.reservation.application;

import kr.hhplus.be.server.reservation.application.dto.HoldSeatCommand;
import kr.hhplus.be.server.reservation.application.dto.HoldSeatResult;

public interface HoldSeatUseCase {
    HoldSeatResult hold(HoldSeatCommand command);
}
