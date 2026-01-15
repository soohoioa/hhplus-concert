package kr.hhplus.be.server.queue;

import kr.hhplus.be.server.queue.infrastructure.QueueRedisRepository;
import kr.hhplus.be.server.support.RedisContainerTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
public class QueueRedisRepositoryConcurrencyTest extends RedisContainerTestBase {

    @Autowired
    QueueRedisRepository queueRedisRepository;

    @Test
    void 동시에_등록해도_rank가_중복없이_0부터_N_1까지_할당된다() throws Exception {
        int n = 50;
        String queueKey = "queue:test:concurrency:" + UUID.randomUUID();

        ExecutorService pool = Executors.newFixedThreadPool(n);
        CountDownLatch startGate = new CountDownLatch(1);
        CountDownLatch doneGate = new CountDownLatch(n);

        ConcurrentLinkedQueue<Long> ranks = new ConcurrentLinkedQueue<>();

        var users = IntStream.range(0, n)
                .mapToObj(i -> "user-" + UUID.randomUUID())
                .toList();

        for (String userUuid : users) {
            pool.submit(() -> {
                try {
                    startGate.await();
                    long rank = queueRedisRepository.registerIfAbsent(queueKey, userUuid);
                    ranks.add(rank);
                } catch (Exception e) {
                    ranks.add(-999L);
                    e.printStackTrace();
                } finally {
                    doneGate.countDown();
                }
            });
        }

        startGate.countDown();
        boolean finished = doneGate.await(10, TimeUnit.SECONDS);
        pool.shutdown();

        assertThat(finished).isTrue();
        assertThat(ranks).hasSize(n);

        Set<Long> unique = ranks.stream().collect(Collectors.toSet());
        assertThat(unique).hasSize(n); // 중복 없음

        long min = unique.stream().min(Long::compareTo).orElseThrow();
        long max = unique.stream().max(Long::compareTo).orElseThrow();
        assertThat(min).isEqualTo(0L);
        assertThat(max).isEqualTo(n - 1L);

        Set<Long> expected = IntStream.range(0, n)
                .mapToObj(i -> (long) i)
                .collect(Collectors.toSet());

        assertThat(unique).containsExactlyInAnyOrderElementsOf(expected);
    }

}
