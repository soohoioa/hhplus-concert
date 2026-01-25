package kr.hhplus.be.server.queue;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
class DummyProtectedController {

    @GetMapping("/api/v1/protected/ping")
    public String ping() {
        return "pong";
    }
}
