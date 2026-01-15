package kr.hhplus.be.server.reservation;

import com.fasterxml.jackson.databind.ObjectMapper;
import kr.hhplus.be.server.queue.application.QueueService;
import kr.hhplus.be.server.queue.support.QueueKeys;
import kr.hhplus.be.server.reservation.application.dto.HoldSeatCommand;
import kr.hhplus.be.server.reservation.application.dto.HoldSeatResult;
import kr.hhplus.be.server.reservation.application.service.HoldSeatUseCase;
import kr.hhplus.be.server.reservation.controller.dto.SeatHoldRequest;
import kr.hhplus.be.server.support.RedisContainerTestBase;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class ReservationQueueIntegrationTest extends RedisContainerTestBase {

    @Autowired
    MockMvc mockMvc;
    @Autowired
    ObjectMapper objectMapper;
    @Autowired
    QueueService queueService;

    @MockitoBean
    HoldSeatUseCase holdSeatUseCase;

    @Test
    void 예약_hold는_대기열토큰이_없으면_401_반환() throws Exception {
        // given
        long scheduleId = 10L;
        SeatHoldRequest req = new SeatHoldRequest(1L, scheduleId, 1);
        // when & then
        mockMvc.perform(post("/api/v1/reservations/hold")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("QUEUE-401"))
                .andExpect(jsonPath("$.path").value("/api/v1/reservations/hold"));

        verifyNoInteractions(holdSeatUseCase);
    }

    @Test
    void 예약_hold는_토큰이_있어도_내차례가_아니면_429_반환() throws Exception {
        long scheduleId = 10L;
        String queueKey = QueueKeys.scheduleQueueKey(scheduleId);

        queueService.issueToken("user-" + UUID.randomUUID(), queueKey);

        QueueService.IssueResult issue = queueService.issueToken("user-" + UUID.randomUUID(), queueKey);
        String queueToken = issue.queueToken(); // rank=1일 확률 매우 높음(첫 유저가 이미 rank=0)

        SeatHoldRequest req = new SeatHoldRequest(1L, scheduleId, 1);

        // when & then
        mockMvc.perform(post("/api/v1/reservations/hold")
                        .header("X-QUEUE-TOKEN", queueToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.code").value("QUEUE-429"))
                .andExpect(jsonPath("$.path").value("/api/v1/reservations/hold"));

        verifyNoInteractions(holdSeatUseCase);
    }

    @Test
    void 예약_hold는_rank_0이면_필터를_통과하고_유스케이스가_호출된다() throws Exception {
        // given
        long scheduleId = 10L;
        String queueKey = QueueKeys.scheduleQueueKey(scheduleId);

        String freshQueueKey = queueKey + ":" + UUID.randomUUID();

        QueueService.IssueResult issue = queueService.issueToken("user-" + UUID.randomUUID(), freshQueueKey);
        String queueToken = issue.queueToken();
        assertThat(issue.rank()).isEqualTo(0L); // 안전장치

        // holdSeatUseCase mock 응답 준비
        HoldSeatResult mockResult = mock(HoldSeatResult.class);
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(5);

        when(mockResult.getScheduleId()).thenReturn(scheduleId);
        when(mockResult.getSeatNo()).thenReturn(1);
        when(mockResult.getHoldExpiresAt()).thenReturn(expiresAt);

        when(holdSeatUseCase.hold(any(HoldSeatCommand.class))).thenReturn(mockResult);

        SeatHoldRequest req = new SeatHoldRequest(1L, scheduleId, 1);

        // when & then
        mockMvc.perform(post("/api/v1/reservations/hold")
                        .header("X-QUEUE-TOKEN", queueToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.scheduleId").value((int) scheduleId))
                .andExpect(jsonPath("$.seatNo").value(1))
                .andExpect(jsonPath("$.holdExpiresAt").exists());

        // 유스케이스 호출 검증 + 커맨드 값 검증
        ArgumentCaptor<HoldSeatCommand> captor = ArgumentCaptor.forClass(HoldSeatCommand.class);
        verify(holdSeatUseCase, times(1)).hold(captor.capture());

        HoldSeatCommand cmd = captor.getValue();
        assertThat(cmd.getUserId()).isEqualTo(1L);
        assertThat(cmd.getScheduleId()).isEqualTo(scheduleId);
        assertThat(cmd.getSeatNo()).isEqualTo(1);
    }

}
