package kr.hhplus.be.server.reservation.infrastructure.persistence.outbox;

import kr.hhplus.be.server.reservation.infrastructure.kafka.ReservationKafkaProducer;
import kr.hhplus.be.server.reservation.infrastructure.persistence.jpa.ReservationOutbox;
import kr.hhplus.be.server.reservation.infrastructure.persistence.jpa.ReservationOutboxJpaRepository;
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
public class ReservationOutboxProcessor {

    private final ReservationOutboxJpaRepository outboxRepository;
    private final ReservationKafkaProducer kafkaProducer;

    @Value("${kafka.topics.reservation-hold-created}")
    private String holdCreatedTopic;

    @Transactional
    @Scheduled(fixedDelayString = "${outbox.reservation.fixed-delay-ms:1000}")
    public void publishPendingEvents() {
        List<ReservationOutbox> pendings = outboxRepository.findTop100ByStatusOrderByIdAsc("PENDING");
        if (pendings.isEmpty()) return;

        for (ReservationOutbox outbox : pendings) {
            publishOne(outbox);
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void publishOne(ReservationOutbox outbox) {
        int updated = outboxRepository.markSending(outbox.getId());
        if (updated == 0) return;

        try {
            kafkaProducer.publish(outbox, holdCreatedTopic);
            outboxRepository.markSent(outbox.getId());
        } catch (Exception e) {
            outboxRepository.markFailed(outbox.getId(), safeMsg(e));
            log.warn("[RESERVATION_OUTBOX] publish failed. eventId={}, retryCount={}, error={}",
                    outbox.getEventId(), outbox.getRetryCount(), e.getMessage());
        }
    }

    private String safeMsg(Exception e) {
        String msg = e.getMessage();
        if (msg == null) return "null";
        return msg.length() > 500 ? msg.substring(0, 500) : msg;
    }
}
