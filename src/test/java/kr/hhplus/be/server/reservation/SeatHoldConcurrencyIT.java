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
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
public class SeatHoldConcurrencyIT { // 다중 유저가 동시에 좌석 요청 시 한 명만 성공하도록 테스트

    @Autowired
    HoldSeatUseCase holdSeatUseCase;
    @Autowired
    ScheduleSeatRepository seatRepository;

    @Autowired
    PlatformTransactionManager txManager;
    @Autowired
    EntityManager entityManager;

    @Test
    void 동시에_같은좌석_hold하면_1명만_성공한다() throws Exception {
        TransactionTemplate tx = new TransactionTemplate(txManager);

        Long scheduleId = tx.execute(status -> {
            Concert concert = Concert.create("test-concert");
            entityManager.persist(concert);

            ConcertSchedule schedule = ConcertSchedule.create(concert, LocalDateTime.now().plusDays(1));
            entityManager.persist(schedule);

            entityManager.persist(ScheduleSeat.create(schedule, 1));

            entityManager.flush(); // ID 생성 + INSERT 반영
            return schedule.getId();
        });

        int seatNo = 1;

        assertThat(seatRepository.findByScheduleIdAndSeatNo(scheduleId, seatNo)).isPresent();

        // when: 동시 hold
        int threadCount = 10;
        ExecutorService pool = Executors.newFixedThreadPool(threadCount);

        CountDownLatch startGate = new CountDownLatch(1);
        CountDownLatch doneGate = new CountDownLatch(threadCount);

        ConcurrentLinkedQueue<Boolean> results = new ConcurrentLinkedQueue<>();

        for (int i = 0; i < threadCount; i++) {
            long userId = i + 1;

            pool.submit(() -> {
                try {
                    startGate.await();

                    Boolean ok = tx.execute(status -> {
                        try {
                            holdSeatUseCase.hold(new HoldSeatCommand(userId, scheduleId, seatNo));
                            return true;
                        } catch (Exception e) {
                            return false;
                        }
                    });

                    results.add(ok);
                } catch (InterruptedException ignored) {
                    results.add(false);
                } finally {
                    doneGate.countDown();
                }
            });
        }

        startGate.countDown();
        doneGate.await();

        pool.shutdown();
        pool.awaitTermination(5, TimeUnit.SECONDS);

        // then
        long success = results.stream().filter(Boolean::booleanValue).count();
        assertThat(success).isEqualTo(1);

        ScheduleSeat latest = seatRepository.findByScheduleIdAndSeatNo(scheduleId, seatNo).orElseThrow();
        assertThat(latest.getStatus()).isEqualTo(SeatStatus.HELD);
        assertThat(latest.getHoldUserId()).isNotNull();
        assertThat(latest.getHoldExpiresAt()).isNotNull();
    }
}
