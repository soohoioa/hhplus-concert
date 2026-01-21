package kr.hhplus.be.server.reservation;

import jakarta.persistence.EntityManager;
import kr.hhplus.be.server.concert.domain.Concert;
import kr.hhplus.be.server.concert.domain.ConcertSchedule;
import kr.hhplus.be.server.reservation.application.dto.HoldSeatCommand;
import kr.hhplus.be.server.reservation.application.service.HoldSeatUseCase;
import kr.hhplus.be.server.reservation.domain.ScheduleSeat;
import kr.hhplus.be.server.reservation.domain.SeatStatus;
import kr.hhplus.be.server.reservation.infrastructure.persistence.jpa.ScheduleSeatRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDateTime;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers
public class SeatHoldDistributedLockIT { // Redis 분산락 통합 테스트 - 좌석 HOLD + Redis 분산락 검증
    /**
     * 목적 : Redis 분산락이 실제로 동시 진입을 막고 있다”를 테스트로 증명, 기존 DB 락 테스트와 의미가 겹치지 않게 보강
     * Redis 락 때문에 비즈니스 로직 진입 자체가 1번만 일어남
     * DB 조건부 UPDATE가 아니라 락 키 단위 제어가 작동함을 보여줌
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
    HoldSeatUseCase holdSeatUseCase;

    @Autowired
    ScheduleSeatRepository seatRepository;

    @Autowired
    EntityManager entityManager;

    @Autowired
    PlatformTransactionManager txManager;

    @Test
    void 동시에_같은좌석_hold하면_분산락으로_1명만_비즈니스로직에_진입한다() throws Exception {
        // given
        ConcertSchedule schedule = createScheduleWithSeat1();

        int threadCount = 10;
        ExecutorService pool = Executors.newFixedThreadPool(threadCount);

        CountDownLatch startGate = new CountDownLatch(1);
        CountDownLatch doneGate = new CountDownLatch(threadCount);

        AtomicInteger success = new AtomicInteger();

        for (int i = 0; i < threadCount; i++) {
            pool.submit(() -> {
                try {
                    startGate.await();
                    holdSeatUseCase.hold(new HoldSeatCommand(1L, schedule.getId(), 1));
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

        ScheduleSeat seat =
                seatRepository.findByScheduleIdAndSeatNo(schedule.getId(), 1).orElseThrow();

        assertThat(seat.getStatus()).isEqualTo(SeatStatus.HELD);
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
}
