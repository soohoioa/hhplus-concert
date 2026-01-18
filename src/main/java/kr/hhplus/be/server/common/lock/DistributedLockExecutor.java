package kr.hhplus.be.server.common.lock;

import kr.hhplus.be.server.common.error.AppException;
import kr.hhplus.be.server.common.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.UUID;
import java.util.function.Supplier;

@Component
@RequiredArgsConstructor
public class DistributedLockExecutor {

    private final RedisLockClient redisLockClient;

    private static final Duration DEFAULT_WAIT_TIMEOUT = Duration.ofMillis(300);
    private static final Duration DEFAULT_RETRY_INTERVAL = Duration.ofMillis(30);

    /**
     * HoldSeatUseCaseImpl에서 executeWithLock(lockKey, ttl, action) 형태로 호출 가능
     */
    public <T> T executeWithLock(
            String lockKey,
            Duration ttl,
            Supplier<T> action
    ) {
        return executeWithLock(
                lockKey,
                ttl,
                DEFAULT_WAIT_TIMEOUT,
                DEFAULT_RETRY_INTERVAL,
                action
        );
    }

    /**
     * 상세 옵션 버전 (필요한 곳에서만 사용)
     */
    public <T> T executeWithLock(
            String lockKey,
            Duration ttl,
            Duration waitTimeout,
            Duration retryInterval,
            Supplier<T> action
    ) {
        String lockValue = UUID.randomUUID().toString();
        long deadline = System.nanoTime() + waitTimeout.toNanos();

        boolean locked = false;
        while (System.nanoTime() < deadline) {
            locked = redisLockClient.tryLock(lockKey, lockValue, ttl);
            if (locked) break;

            try {
                Thread.sleep(retryInterval.toMillis());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        if (!locked) {
            throw new AppException(ErrorCode.SEAT_HOLD_CONFLICT);
        }

        try {
            return action.get();
        } finally {
            redisLockClient.unlock(lockKey, lockValue);
        }
    }
}
