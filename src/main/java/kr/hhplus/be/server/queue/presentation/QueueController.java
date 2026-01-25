package kr.hhplus.be.server.queue.presentation;

import kr.hhplus.be.server.queue.application.QueueService;
import kr.hhplus.be.server.queue.support.QueueKeys;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/queue")
public class QueueController {

    private final QueueService queueService;

    /**
     * 대기열 토큰 발급
     * - scheduleId 기준 대기열로 가정 (콘서트 예약 흐름이랑 자연스럽게 연결됨)
     */
    @PostMapping("/token")
    public QueueService.IssueResult issue(@RequestParam String userUuid,
                                          @RequestParam Long scheduleId) {
        return queueService.issueToken(userUuid, scheduleId);
    }

    /**
     * 대기번호 조회(폴링)
     */
    @GetMapping("/status")
    public QueueService.StatusResult status(@RequestHeader("X-QUEUE-TOKEN") String token) {
        return queueService.getStatus(token);
    }
}
