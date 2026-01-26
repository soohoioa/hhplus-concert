package kr.hhplus.be.server.payment.infrastructure.persistence.adapter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import kr.hhplus.be.server.common.error.AppException;
import kr.hhplus.be.server.common.error.ErrorCode;
import kr.hhplus.be.server.payment.application.event.PaymentCompletedEvent;
import kr.hhplus.be.server.payment.infrastructure.persistence.jpa.PaymentOutbox;
import kr.hhplus.be.server.payment.infrastructure.persistence.jpa.PaymentOutboxJpaRepository;
import kr.hhplus.be.server.payment.port.out.SavePaymentOutboxPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PaymentOutboxJpaAdapter implements SavePaymentOutboxPort {

    private final PaymentOutboxJpaRepository repository;
    private final ObjectMapper objectMapper;

    @Override
    public void save(PaymentCompletedEvent event) {
        repository.save(PaymentOutbox.pending(
                event.getClass().getSimpleName(),
                toJson(event)
        ));
    }

    private String toJson(PaymentCompletedEvent event) {
        try {
            return objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException e) {
            throw new AppException(ErrorCode.PAYMENT_OUTBOX_SERIALIZATION_FAILED, e.getMessage());
        }
    }
}
