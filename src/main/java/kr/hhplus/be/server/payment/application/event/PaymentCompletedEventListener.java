package kr.hhplus.be.server.payment.application.event;

import kr.hhplus.be.server.payment.port.out.SavePaymentOutboxPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

@RequiredArgsConstructor
@Component
public class PaymentCompletedEventListener {

    private final SavePaymentOutboxPort savePaymentOutboxPort;

    @TransactionalEventListener
    public void on(PaymentCompletedEvent event) {
        savePaymentOutboxPort.save(event);
    }
}
