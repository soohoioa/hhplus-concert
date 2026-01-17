package kr.hhplus.be.server.queue;

import kr.hhplus.be.server.support.RedisContainerTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
public class QueueControllerTest extends RedisContainerTestBase {

    @Autowired
    MockMvc mockMvc;

    @Test
    void 토큰을_발급받고_상태조회_폴링을_할_수_있다() throws Exception {
        String userUuid = "user-" + UUID.randomUUID();
        long scheduleId = 1L;

        // 1) 토큰 발급
        String tokenJson = mockMvc.perform(post("/api/v1/queue/token")
                        .param("userUuid", userUuid)
                        .param("scheduleId", String.valueOf(scheduleId))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.queueToken", not(emptyString())))
                .andExpect(jsonPath("$.rank", greaterThanOrEqualTo(0)))
                .andExpect(jsonPath("$.etaSeconds", greaterThanOrEqualTo(0)))
                .andReturn()
                .getResponse()
                .getContentAsString();

        String queueToken = tokenJson.split("\"queueToken\":\"")[1].split("\"")[0];

        // 2) 상태 조회(폴링)
        mockMvc.perform(get("/api/v1/queue/status")
                        .header("X-QUEUE-TOKEN", queueToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rank", greaterThanOrEqualTo(0)))
                .andExpect(jsonPath("$.etaSeconds", greaterThanOrEqualTo(0)))
                .andExpect(jsonPath("$.ready", anyOf(is(true), is(false))));
    }
}
