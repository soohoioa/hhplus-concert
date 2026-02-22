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
    // Permit (입장권) 관리 - B 방식 (TTL 키 단일 관리)
    // ---------------------------

    /**
     * permit TTL 키 하나만 존재하면 permit 유효로 본다.
     */
    public void grantPermit(String permitTtlKey, Duration ttl) {
        stringRedisTemplate.opsForValue().set(permitTtlKey, "1", ttl);
    }

    /**
     * TTL 키 존재 여부로 permit 유효성 판단
     */
    public boolean hasValidPermit(String permitTtlKey) {
        Boolean ttlExists = stringRedisTemplate.hasKey(permitTtlKey);
        return ttlExists != null && ttlExists;
    }

    public void revokePermit(String permitTtlKey) {
        stringRedisTemplate.delete(permitTtlKey);
    }

    /**
     * (레거시 호환) 기존 시그니처를 유지하되 내부적으로 TTL 키 방식만 사용
     * - permitKey / userUuid는 더 이상 사용하지 않음
     */
    @Deprecated
    public void grantPermit(String permitKey, String permitTtlKey, String userUuid, Duration ttl) {
        grantPermit(permitTtlKey, ttl);
    }

    @Deprecated
    public boolean hasValidPermit(String permitKey, String permitTtlKey, String userUuid) {
        return hasValidPermit(permitTtlKey);
    }

    @Deprecated
    public void revokePermit(String permitKey, String permitTtlKey, String userUuid) {
        revokePermit(permitTtlKey);
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