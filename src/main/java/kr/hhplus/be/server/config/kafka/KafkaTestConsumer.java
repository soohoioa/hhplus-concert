package kr.hhplus.be.server.config.kafka;

import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class KafkaTestConsumer {

    @KafkaListener(topics = "${kafka.topics.test:test-topic}", groupId = "hhplus-local")
    public void consume(TestEvent event, Acknowledgment ack) {
        log.info("✅ consumed event = {}", event);
        ack.acknowledge();
    }
}
