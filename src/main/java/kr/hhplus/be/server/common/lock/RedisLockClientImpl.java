package kr.hhplus.be.server.common.lock;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.connection.ReturnType;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;

@Component
@RequiredArgsConstructor
public class RedisLockClientImpl implements RedisLockClient {

    private final StringRedisTemplate stringRedisTemplate;

    private static final DefaultRedisScript<Long> UNLOCK_SCRIPT = new DefaultRedisScript<>(
            """
            if redis.call('get', KEYS[1]) == ARGV[1] then
                return redis.call('del', KEYS[1])
            else
                return 0
            end
            """,
            Long.class
    );

    @Override
    public boolean tryLock(String key, String value, Duration ttl) {
        Boolean success = stringRedisTemplate.opsForValue().setIfAbsent(key, value, ttl);
        return Boolean.TRUE.equals(success);
    }

    @Override
    public boolean unlock(String key, String value) {
        Long result = stringRedisTemplate.execute(UNLOCK_SCRIPT, List.of(key), value);
        return result != null && result > 0;
    } // 락 소유자가 아닌 프로세스가 락을 해제하는 걸 방지하기 위해 Lua unlock
}
