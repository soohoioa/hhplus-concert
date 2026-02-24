package kr.hhplus.be.server.payment.application.event;

import kr.hhplus.be.server.payment.port.out.SavePaymentOutboxPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Slf4j
@RequiredArgsConstructor
@Component
public class PaymentCompletedEventListener {

    private final SavePaymentOutboxPort savePaymentOutboxPort;

    @EventListener
    public void on(PaymentCompletedEvent event) {
        log.info("[PAYMENT_EVENT] listener fired paymentId={}", event.getPaymentId());

        try {
            savePaymentOutboxPort.save(event);
            log.info("[PAYMENT_EVENT] outbox saved paymentId={}", event.getPaymentId());
        } catch (Exception e) {
            log.error("[PAYMENT_EVENT] outbox save FAILED paymentId={}", event.getPaymentId(), e);
        }
    }
}