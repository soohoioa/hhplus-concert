package kr.hhplus.be.server.queue.application;

import kr.hhplus.be.server.queue.infrastructure.QueueRedisRepository;
import kr.hhplus.be.server.queue.support.QueueKeys;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SoldoutRankingService {

    private final QueueRedisRepository queueRedisRepository;

    /**
     * 스케줄 오픈 시점 기록 (없으면 기록)
     */
    public void markOpenIfAbsent(Long scheduleId) {
        queueRedisRepository.setOpenAtIfAbsent(QueueKeys.scheduleOpenAtKey(scheduleId), System.currentTimeMillis());
    }

    /**
     * 매진 발생 시 랭킹 기록
     * score = (now - openAt) / 1000.0
     */
    public void recordSoldout(Long scheduleId) {
        Long openAt = queueRedisRepository.getOpenAtMillis(QueueKeys.scheduleOpenAtKey(scheduleId));
        if (openAt == null) {
            // openAt이 없으면 지금을 open으로 간주(최소 안전장치)
            openAt = System.currentTimeMillis();
            queueRedisRepository.setOpenAtIfAbsent(QueueKeys.scheduleOpenAtKey(scheduleId), openAt);
        }

        long now = System.currentTimeMillis();
        double durationSeconds = Math.max(0, (now - openAt) / 1000.0);

        queueRedisRepository.recordSoldout(QueueKeys.soldoutRankKey(), scheduleId, durationSeconds);
    }
}
