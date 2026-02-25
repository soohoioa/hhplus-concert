package kr.hhplus.be.server.config.kafka;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/kafka")
public class KafkaTestController {

    private final KafkaTestProducer producer;

    @PostMapping("/send")
    public String send(@RequestParam String message) {
        producer.send(message);
        return "sent: " + message;
    }
}
