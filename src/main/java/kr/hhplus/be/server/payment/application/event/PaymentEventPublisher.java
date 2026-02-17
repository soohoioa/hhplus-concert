package kr.hhplus.be.server.payment.application.event;

public interface PaymentEventPublisher {
    void publish(PaymentCompletedEvent event);
}
