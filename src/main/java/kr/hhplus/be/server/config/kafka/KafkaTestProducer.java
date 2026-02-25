package kr.hhplus.be.server.config.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class KafkaTestProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Value("${kafka.topics.test:test-topic}")
    private String topic;

    public void send(String message) {
        TestEvent event = new TestEvent(System.currentTimeMillis(), message);
        kafkaTemplate.send(topic, String.valueOf(event.id()), toJson(event));
    }

    private String toJson(TestEvent event) {
        try {
            return objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize TestEvent", e);
        }
    }
}