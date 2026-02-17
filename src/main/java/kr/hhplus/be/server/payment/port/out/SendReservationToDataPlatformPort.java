package kr.hhplus.be.server.payment.port.out;

import kr.hhplus.be.server.payment.application.event.PaymentCompletedEvent;

public interface SendReservationToDataPlatformPort {
    void send(PaymentCompletedEvent event);
}
