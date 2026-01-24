package kr.hhplus.be.server.queue.support;

public final class QueueKeys {

    private QueueKeys() {}

    // 스케줄 단위 대기열 키
    public static String scheduleQueueKey(Long scheduleId) {
        return "queue:schedule:" + scheduleId;
    }

    // permit(입장 허용) 집합 (SET)
    public static String schedulePermitKey(Long scheduleId) {
        return "queue:permit:schedule:" + scheduleId;
    }

    // permit TTL 관리용 (STRING with TTL)
    public static String schedulePermitTtlKey(Long scheduleId, String userUuid) {
        return "queue:permit:ttl:schedule:" + scheduleId + ":" + userUuid;
    }

    // (랭킹) 스케줄 오픈 시간 기록 (STRING)
    public static String scheduleOpenAtKey(Long scheduleId) {
        return "schedule:openAt:" + scheduleId;
    }

    // (랭킹) 빠른 매진 랭킹 (ZSET)
    public static String soldoutRankKey() {
        return "rank:soldout";
    }

    // 전역 대기열이 필요하면 사용
    public static String globalQueueKey() {
        return "queue:global";
    }
}
