package kr.hhplus.be.server.reservation.infrastructure.kafka;

import kr.hhplus.be.server.reservation.infrastructure.persistence.jpa.ReservationOutbox;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ReservationKafkaProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;

    public void publish(ReservationOutbox outbox, String topic) {
        try {
            kafkaTemplate.send(
                    topic,
                    outbox.getEventId(), // Payment와 동일하게 eventId를 key로 사용
                    outbox.getPayload()
            ).get();
        } catch (Exception e) {
            throw new IllegalStateException(
                    "Kafka publish failed. eventId=" + outbox.getEventId(), e
            );
        }
    }
}