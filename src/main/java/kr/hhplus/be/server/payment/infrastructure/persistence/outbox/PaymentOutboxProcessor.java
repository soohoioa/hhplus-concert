package kr.hhplus.be.server.payment.infrastructure.persistence.outbox;

import kr.hhplus.be.server.payment.infrastructure.kafka.PaymentKafkaProducer;
import kr.hhplus.be.server.payment.infrastructure.persistence.jpa.PaymentOutbox;
import kr.hhplus.be.server.payment.infrastructure.persistence.jpa.PaymentOutboxJpaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentOutboxProcessor {
    private final PaymentOutboxJpaRepository outboxRepository;
    private final PaymentKafkaProducer kafkaProducer;

    @Value("${kafka.topics.payment-completed}")
    private String paymentCompletedTopic;

    @Transactional
    @Scheduled(fixedDelayString = "${outbox.payment.fixed-delay-ms:1000}")
    public void publishPendingEvents() {

        List<PaymentOutbox> pendings = outboxRepository.findTop100ByStatusOrderByIdAsc("PENDING");
        if (pendings.isEmpty()) {
            return;
        }

        for (PaymentOutbox outbox : pendings) {
            publishOne(outbox);
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void publishOne(PaymentOutbox outbox) {

        int updated = outboxRepository.markSending(outbox.getId());
        if (updated == 0) {
            return; // 다른 인스턴스가 선점했거나 이미 처리됨
        }

        try {
            kafkaProducer.publish(outbox, paymentCompletedTopic);
            //outbox.markSent();
            outboxRepository.markSent(outbox.getId());
        } catch (Exception e) {
            //outbox.markFailed(e.getMessage());
            outboxRepository.markFailed(outbox.getId(), safeMsg(e));
            log.warn("[PAYMENT_OUTBOX] publish failed. eventId={}, retryCount={}, error={}",
                    outbox.getEventId(), outbox.getRetryCount(), e.getMessage());
        }
    }

    private String safeMsg(Exception e) {
        String msg = e.getMessage();
        if (msg == null) return "null";
        return msg.length() > 500 ? msg.substring(0, 500) : msg;
    }

}
