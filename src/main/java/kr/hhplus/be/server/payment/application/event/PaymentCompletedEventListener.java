package kr.hhplus.be.server.payment.application.event;

import kr.hhplus.be.server.payment.port.out.SendReservationToDataPlatformPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@RequiredArgsConstructor
@Component
public class PaymentCompletedEventListener {

    private final SendReservationToDataPlatformPort sendReservationToDataPlatformPort;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void on(PaymentCompletedEvent event) {
        sendReservationToDataPlatformPort.send(event);
    }
}
