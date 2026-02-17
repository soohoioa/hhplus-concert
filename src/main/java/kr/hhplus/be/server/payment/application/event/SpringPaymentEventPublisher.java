package kr.hhplus.be.server.payment.application.event;

import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class SpringPaymentEventPublisher implements PaymentEventPublisher {

    private final ApplicationEventPublisher publisher;

    @Override
    public void publish(PaymentCompletedEvent event) {
        publisher.publishEvent(event);
    }
}
