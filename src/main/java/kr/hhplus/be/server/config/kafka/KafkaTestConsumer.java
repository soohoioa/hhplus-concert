package kr.hhplus.be.server.config.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaTestConsumer {

    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "${kafka.topics.test:test-topic}")
    public void consume(String payload, Acknowledgment ack) {
        try {
            TestEvent event = objectMapper.readValue(payload, TestEvent.class);
            log.info("✅ consumed event = {}", event);
            ack.acknowledge();
        } catch (JsonProcessingException e) {
            // 테스트 소비자라면 로그만 남기고 ack 처리 여부는 선택
            log.error("❌ failed to parse TestEvent payload={}", payload, e);

            // 1) 재처리 원하면 ack 하지 않기 (같은 메시지 계속 재시도될 수 있음)
            // return;

            // 2) 스모크 테스트면 무한 재시도 피하려면 ack 하고 넘기기
            ack.acknowledge();
        }
    }
}