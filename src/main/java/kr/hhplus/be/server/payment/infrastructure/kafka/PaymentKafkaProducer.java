package kr.hhplus.be.server.payment.infrastructure.kafka;

import kr.hhplus.be.server.payment.infrastructure.persistence.jpa.PaymentOutbox;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PaymentKafkaProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;

    public void publish(PaymentOutbox outbox, String topic) {
        try {
            kafkaTemplate.send(topic, outbox.getEventId(), outbox.getPayload()).get();
        } catch (Exception e) {
            throw new IllegalStateException("Kafka publish failed. eventId=" + outbox.getEventId(), e);
        }
    }
}
