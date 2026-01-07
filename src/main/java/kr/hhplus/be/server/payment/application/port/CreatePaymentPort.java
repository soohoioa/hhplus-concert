package kr.hhplus.be.server.payment.application.port;

import kr.hhplus.be.server.payment.domain.Payment;

public interface CreatePaymentPort {
    Payment save(Payment payment);
}
