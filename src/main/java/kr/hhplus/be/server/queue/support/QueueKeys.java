package kr.hhplus.be.server.queue.support;

public final class QueueKeys {

    private QueueKeys() {}

    // 스케줄 단위 대기열 키
    public static String scheduleQueueKey(Long scheduleId) {
        return "queue:schedule:" + scheduleId;
    }

    // 전역 대기열이 필요하면 사용
    public static String globalQueueKey() {
        return "queue:global";
    }
}
