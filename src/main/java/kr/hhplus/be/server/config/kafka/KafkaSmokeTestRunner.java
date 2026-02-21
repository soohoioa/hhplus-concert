package kr.hhplus.be.server.config.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Profile({"local", "test"}) // 로컬에서만 자동 발행
@RequiredArgsConstructor
public class KafkaSmokeTestRunner implements ApplicationRunner {

    private final KafkaTestProducer producer;

    @Override
    public void run(ApplicationArguments args) {
        String msg = "boot-smoke-test";
        log.info("🚀 producing smoke message: {}", msg);
        producer.send(msg);
    }
}
