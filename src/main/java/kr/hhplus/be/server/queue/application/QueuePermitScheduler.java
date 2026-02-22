package kr.hhplus.be.server.queue.application;

import kr.hhplus.be.server.queue.infrastructure.QueueRedisRepository;
import kr.hhplus.be.server.queue.support.QueueKeys;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class QueuePermitScheduler {

    private final QueueRedisRepository queueRedisRepository;

    @Value("${queue.permit.batch-size:1}")
    private int batchSize;

    @Value("${queue.permit.ttl-seconds:60}")
    private long permitTtlSeconds;

    private static final String ACTIVE_SCHEDULES_KEY = "queue:schedules:active";

    @Scheduled(fixedDelayString = "${queue.permit.scheduler.delay-ms:200}")
    public void grantPermits() {
        Set<String> scheduleIds = queueRedisRepository.getActiveSchedules(ACTIVE_SCHEDULES_KEY);
        if (scheduleIds.isEmpty()) return;

        for (String scheduleIdStr : scheduleIds) {
            Long scheduleId;
            try { scheduleId = Long.parseLong(scheduleIdStr); }
            catch (NumberFormatException e) { continue; }

            String queueKey = QueueKeys.scheduleQueueKey(scheduleId);

            Set<String> topUsers = queueRedisRepository.peekTopN(queueKey, batchSize);
            if (topUsers.isEmpty()) continue;

            for (String userUuid : topUsers) {
                String permitTtlKey = QueueKeys.schedulePermitTtlKey(scheduleId, userUuid);

                // TTL 키 존재하면 이미 permit 유효
                if (queueRedisRepository.hasValidPermit(permitTtlKey)) continue;

                queueRedisRepository.grantPermit(permitTtlKey, Duration.ofSeconds(permitTtlSeconds));
                queueRedisRepository.remove(queueKey, userUuid);
            }
        }
    }
}