package kr.hhplus.be.server.config.kafka;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class KafkaTestProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${kafka.topics.test:test-topic}")
    private String topic;

    public void send(String message) {
        TestEvent event = new TestEvent(System.currentTimeMillis(), message);
        kafkaTemplate.send(topic, String.valueOf(event.id()), event);
    }
}
