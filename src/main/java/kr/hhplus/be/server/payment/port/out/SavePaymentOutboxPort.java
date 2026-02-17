package kr.hhplus.be.server.payment.port.out;

import kr.hhplus.be.server.payment.application.event.PaymentCompletedEvent;

public interface SavePaymentOutboxPort {
    void save(PaymentCompletedEvent event);
}
