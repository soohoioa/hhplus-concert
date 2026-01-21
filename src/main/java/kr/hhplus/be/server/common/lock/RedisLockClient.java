package kr.hhplus.be.server.common.lock;

import java.time.Duration;

public interface RedisLockClient {

    boolean tryLock(String key, String value, Duration ttl);

    boolean unlock(String key, String value);
}
