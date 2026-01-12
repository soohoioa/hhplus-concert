package kr.hhplus.be.server.queue.infrastructure;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class RedisQueueRepository {

    private final StringRedisTemplate stringRedisTemplate;

    /**
     * 이미 대기열에 있으면 그대로 두고, 없으면 score(시간)로 등록
     */
    public long registerIfAbsent(String queueKey, String userUuid) {
        stringRedisTemplate.opsForZSet().addIfAbsent(queueKey, userUuid, System.currentTimeMillis());
        Long rank = stringRedisTemplate.opsForZSet().rank(queueKey, userUuid);
        return rank == null ? -1 : rank;
    }

    public Long getRank(String queueKey, String userUuid) {
        return stringRedisTemplate.opsForZSet().rank(queueKey, userUuid);
    }

    public Long getSize(String queueKey) {
        return stringRedisTemplate.opsForZSet().zCard(queueKey);
    }

    public void remove(String queueKey, String userUuid) {
        stringRedisTemplate.opsForZSet().remove(queueKey, userUuid);
    }

}
