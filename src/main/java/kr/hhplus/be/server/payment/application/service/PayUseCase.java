package kr.hhplus.be.server.payment.application.service;

import kr.hhplus.be.server.payment.application.dto.PayCommand;
import kr.hhplus.be.server.payment.application.dto.PayResult;

public interface PayUseCase {
    PayResult pay(PayCommand command);
}
