package kr.hhplus.be.server.queue.infrastructure;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.util.Collections;
import java.util.Set;

@Repository
@RequiredArgsConstructor
public class QueueRedisRepository {

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

    /**
     * 대기열 상위 N명 조회 (0 ~ n-1)
     */
    public Set<String> peekTopN(String queueKey, int n) {
        if (n <= 0) return Collections.emptySet();
        Set<String> users = stringRedisTemplate.opsForZSet().range(queueKey, 0, n - 1);
        return users == null ? Collections.emptySet() : users;
    }

    // ---------------------------
    // Permit (입장권) 관리
    // ---------------------------

    public void grantPermit(String permitKey, String permitTtlKey, String userUuid, Duration ttl) {
        // permit set 등록
        stringRedisTemplate.opsForSet().add(permitKey, userUuid);

        // TTL 키 (만료되면 permit도 정리 대상)
        stringRedisTemplate.opsForValue().set(permitTtlKey, "1", ttl);
    }

    public boolean hasValidPermit(String permitKey, String permitTtlKey, String userUuid) {
        Boolean inSet = stringRedisTemplate.opsForSet().isMember(permitKey, userUuid);
        if (inSet == null || !inSet) return false;

        Boolean ttlExists = stringRedisTemplate.hasKey(permitTtlKey);
        return ttlExists != null && ttlExists;
    }

    public void revokePermit(String permitKey, String permitTtlKey, String userUuid) {
        stringRedisTemplate.opsForSet().remove(permitKey, userUuid);
        stringRedisTemplate.delete(permitTtlKey);
    }

    public void addActiveSchedule(String activeKey, Long scheduleId) {
        stringRedisTemplate.opsForSet().add(activeKey, String.valueOf(scheduleId));
    }

    public Set<String> getActiveSchedules(String activeKey) {
        Set<String> v = stringRedisTemplate.opsForSet().members(activeKey);
        return v == null ? Collections.emptySet() : v;
    }

    // ---------------------------
    // (랭킹) 오픈시간 / 매진랭킹
    // ---------------------------

    public void setOpenAtIfAbsent(String openAtKey, long epochMillis) {
        stringRedisTemplate.opsForValue().setIfAbsent(openAtKey, String.valueOf(epochMillis));
    }

    public Long getOpenAtMillis(String openAtKey) {
        String v = stringRedisTemplate.opsForValue().get(openAtKey);
        if (v == null) return null;
        try {
            return Long.parseLong(v);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public void recordSoldout(String soldoutRankKey, Long scheduleId, double durationSeconds) {
        // score: durationSeconds (낮을수록 빠른 매진)
        stringRedisTemplate.opsForZSet().add(soldoutRankKey, String.valueOf(scheduleId), durationSeconds);
    }

    public Set<String> getTopSoldout(String soldoutRankKey, int limit) {
        if (limit <= 0) return Collections.emptySet();
        Set<String> ids = stringRedisTemplate.opsForZSet().range(soldoutRankKey, 0, limit - 1);
        return ids == null ? Collections.emptySet() : ids;
    }
}
