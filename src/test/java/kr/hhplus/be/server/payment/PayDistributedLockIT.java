package kr.hhplus.be.server.payment;

import jakarta.persistence.EntityManager;
import kr.hhplus.be.server.concert.domain.Concert;
import kr.hhplus.be.server.concert.domain.ConcertSchedule;
import kr.hhplus.be.server.payment.application.dto.PayCommand;
import kr.hhplus.be.server.payment.application.service.PayUseCase;
import kr.hhplus.be.server.payment.infrastructure.persistence.jpa.PaymentJpaRepository;
import kr.hhplus.be.server.point.domain.UserPoint;
import kr.hhplus.be.server.queue.support.QueueKeys;
import kr.hhplus.be.server.reservation.application.dto.HoldSeatCommand;
import kr.hhplus.be.server.reservation.application.service.HoldSeatUseCase;
import kr.hhplus.be.server.reservation.domain.ScheduleSeat;
import kr.hhplus.be.server.reservation.domain.SeatStatus;
import kr.hhplus.be.server.reservation.infrastructure.persistence.jpa.ScheduleSeatRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;

import java.time.LocalDateTime;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
public class PayDistributedLockIT { // Redis 분산락 통합 테스트 - 결제 PAY + Redis 분산락 검증 + 매진 랭킹 기록 검증
    /**
     * 동일 좌석에 대해 결제 로직 진입 자체가 1번
     * Payment row가 1건만 생성
     */

    @Container
    static GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine")
            .withExposedPorts(6379);

    @DynamicPropertySource
    static void redisProps(DynamicPropertyRegistry registry) {
        registry.add("spring.redis.host", redis::getHost);
        registry.add("spring.redis.port", () -> redis.getMappedPort(6379));
    }

    @Autowired
    PayUseCase payUseCase;

    @Autowired
    HoldSeatUseCase holdSeatUseCase;

    @Autowired
    ScheduleSeatRepository seatRepository;

    @Autowired
    PaymentJpaRepository paymentRepository;

    @Autowired
    EntityManager entityManager;

    @Autowired
    PlatformTransactionManager txManager;

    @Autowired
    StringRedisTemplate stringRedisTemplate;

    @BeforeEach
    void flushRedis() {
        var conn = stringRedisTemplate.getConnectionFactory().getConnection();
        conn.serverCommands().flushAll();
        conn.close();
    }

    @Test
    void 동시에_같은좌석_pay하면_분산락으로_1명만_결제된다() throws Exception {
        // given
        ConcertSchedule schedule = createScheduleWithSeat1();

        // 포인트 준비
        initPoint(1L, 10_000L);

        // 먼저 hold
        holdSeatUseCase.hold(new HoldSeatCommand(1L, schedule.getId(), 1));

        int threadCount = 10;
        ExecutorService pool = Executors.newFixedThreadPool(threadCount);

        CountDownLatch startGate = new CountDownLatch(1);
        CountDownLatch doneGate = new CountDownLatch(threadCount);

        AtomicInteger success = new AtomicInteger();

        for (int i = 0; i < threadCount; i++) {
            pool.submit(() -> {
                try {
                    startGate.await();
                    payUseCase.pay(new PayCommand(1L, schedule.getId(), 1, 1_000L));
                    success.incrementAndGet();
                } catch (Exception ignored) {
                } finally {
                    doneGate.countDown();
                }
            });
        }

        startGate.countDown();
        doneGate.await();
        pool.shutdown();

        // then
        assertThat(success.get()).isEqualTo(1);
        assertThat(paymentRepository.count()).isEqualTo(1);

        ScheduleSeat seat =
                seatRepository.findByScheduleIdAndSeatNo(schedule.getId(), 1).orElseThrow();
        assertThat(seat.getStatus()).isEqualTo(SeatStatus.RESERVED);

        // 매진 랭킹 기록 검증
        Double score = stringRedisTemplate.opsForZSet()
                .score(QueueKeys.soldoutRankKey(), String.valueOf(schedule.getId()));

        assertThat(score).isNotNull();
        assertThat(score).isGreaterThanOrEqualTo(0.0);
    }

    private ConcertSchedule createScheduleWithSeat1() {
        return new TransactionTemplate(txManager).execute(status -> {
            Concert concert = Concert.create("concert");
            entityManager.persist(concert);

            ConcertSchedule schedule =
                    ConcertSchedule.create(concert, LocalDateTime.now().plusDays(1));
            entityManager.persist(schedule);

            entityManager.persist(ScheduleSeat.create(schedule, 1));

            entityManager.flush();
            entityManager.clear();
            return schedule;
        });
    }

    private void initPoint(Long userId, long amount) {
        new TransactionTemplate(txManager).execute(status -> {
            UserPoint point = UserPoint.init(userId);
            point.charge(amount);
            entityManager.persist(point);
            return null;
        });
    }

}
