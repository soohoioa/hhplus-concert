package kr.hhplus.be.server.queue;

import com.fasterxml.jackson.databind.ObjectMapper;
import kr.hhplus.be.server.queue.application.QueueService;
import kr.hhplus.be.server.support.RedisContainerTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class QueueTokenFilterTest extends RedisContainerTestBase {

    @Autowired
    MockMvc mockMvc;
    @Autowired
    QueueService queueService;
    @Autowired
    ObjectMapper objectMapper;

    @Test
    void 토큰이_없으면_401_에러응답이_내려간다() throws Exception {
        mockMvc.perform(get("/api/v1/protected/ping"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("QUEUE-401"))
                .andExpect(jsonPath("$.path").value("/api/v1/protected/ping"));
    }

    @Test
    void 토큰이_있어도_차례가_아니면_429가_내려간다() throws Exception {
        String userUuid = "user-" + UUID.randomUUID();
        String queueKey = "queue:test:filter:" + UUID.randomUUID();

        // 두 명 넣어서 두 번째는 rank=1이 되게 만들기
        queueService.issueToken("user-" + UUID.randomUUID(), queueKey);
        var issue = queueService.issueToken(userUuid, queueKey);

        mockMvc.perform(get("/api/v1/protected/ping")
                        .header("X-QUEUE-TOKEN", issue.queueToken()))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.code").value("QUEUE-429"));
    }

}
