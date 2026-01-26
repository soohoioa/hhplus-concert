package kr.hhplus.be.server.payment.infrastructure.dataplatform;

import kr.hhplus.be.server.payment.application.event.PaymentCompletedEvent;
import kr.hhplus.be.server.payment.port.out.SendReservationToDataPlatformPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class DataPlatformApiAdapter implements SendReservationToDataPlatformPort {

    @Override
    public void send(PaymentCompletedEvent event) {
        // TODO: mock API 호출 (WebClient/RestTemplate로 교체 가능)
        log.info("[DataPlatform] reservation sent: paymentId={}, userId={}, scheduleId={}, seatNo={}, amount={}, paidAt={}",
                event.getPaymentId(), event.getUserId(), event.getScheduleId(), event.getSeatNo(), event.getAmount(), event.getPaidAt());
    }
}
