package kr.hhplus.be.server.queue;

import kr.hhplus.be.server.queue.application.QueuePermitScheduler;
import kr.hhplus.be.server.queue.application.QueueService;
import kr.hhplus.be.server.support.RedisContainerTestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "queue.permit.batch-size=1",
        "queue.permit.ttl-seconds=1", // TTL 짧게 잡아서 만료 테스트
        "queue.permit.scheduler.delay-ms=100000" // 테스트에서는 직접 호출하므로 의미 없음
})
class QueuePermitSchedulerIT extends RedisContainerTestBase {

    @Autowired
    QueueService queueService;
    @Autowired
    QueuePermitScheduler queuePermitScheduler;
    @Autowired
    StringRedisTemplate stringRedisTemplate;

    @BeforeEach
    void flushRedis() {
        var conn = stringRedisTemplate.getConnectionFactory().getConnection();
        conn.serverCommands().flushAll();
        conn.close();
    }

    @Test
    void permit_배치사이즈_1이면_스케줄러_1회_실행시_맨앞_1명만_ready_true가_된다() {
        Long scheduleId = 1L;

        // 3명 등록
        String t1 = queueService.issueToken("u1", scheduleId).queueToken();
        String t2 = queueService.issueToken("u2", scheduleId).queueToken();
        String t3 = queueService.issueToken("u3", scheduleId).queueToken();

        // permit 발급
        queuePermitScheduler.grantPermits();

        assertThat(queueService.getStatus(t1).ready()).isTrue();
        assertThat(queueService.getStatus(t2).ready()).isFalse();
        assertThat(queueService.getStatus(t3).ready()).isFalse();
    }

    @Test
    void permit_TTL_만료되면_다음유저에게_ready가_넘어간다() throws Exception {
        Long scheduleId = 1L;

        String t1 = queueService.issueToken("u1", scheduleId).queueToken();
        String t2 = queueService.issueToken("u2", scheduleId).queueToken();

        // 1) u1에게 permit 부여
        queuePermitScheduler.grantPermits();
        assertThat(queueService.getStatus(t1).ready()).isTrue();
        assertThat(queueService.getStatus(t2).ready()).isFalse();

        // 2) permit TTL 만료 대기 (ttl=1s)
        Thread.sleep(1100);

        // 3) 다시 permit 부여 -> u2에게 넘어가야 함
        queuePermitScheduler.grantPermits();

        assertThat(queueService.getStatus(t1).ready()).isFalse();
        assertThat(queueService.getStatus(t2).ready()).isTrue();
    }
}
