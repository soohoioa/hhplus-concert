package kr.hhplus.be.server.reservation.service;

import kr.hhplus.be.server.common.error.ErrorCode;
import kr.hhplus.be.server.concert.domain.Concert;
import kr.hhplus.be.server.concert.domain.ConcertSchedule;
import kr.hhplus.be.server.concert.infrastructure.persistence.jpa.ConcertJpaRepository;
import kr.hhplus.be.server.concert.infrastructure.persistence.jpa.ConcertScheduleJpaRepository;
import kr.hhplus.be.server.payment.application.dto.PayCommand;
import kr.hhplus.be.server.payment.application.service.PayUseCase;
import kr.hhplus.be.server.point.domain.UserPoint;
import kr.hhplus.be.server.point.infrastructure.persistence.jpa.UserPointJpaRepository;
import kr.hhplus.be.server.reservation.application.dto.HoldSeatCommand;
import kr.hhplus.be.server.reservation.application.service.HoldSeatUseCase;
import kr.hhplus.be.server.reservation.domain.ScheduleSeat;
import kr.hhplus.be.server.reservation.infrastructure.persistence.jpa.ScheduleSeatRepository;
import kr.hhplus.be.server.support.ConcurrencyTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class HoldPayRaceTest extends ConcurrencyTestSupport {

    @Autowired
    PayUseCase payUseCase;
    @Autowired
    HoldSeatUseCase holdSeatUseCase;

    @Autowired
    UserPointJpaRepository userPointJpaRepository;
    @Autowired
    ScheduleSeatRepository seatRepository;

    @Autowired
    ConcertScheduleJpaRepository scheduleJpaRepository;
    @Autowired
    ConcertJpaRepository concertJpaRepository;

    @Autowired
    TransactionTemplate transactionTemplate;

    private ConcertSchedule createScheduleTxCommitted() {
        return transactionTemplate.execute(status -> {
            Concert concert = concertJpaRepository.save(Concert.create("콘서트"));
            return scheduleJpaRepository.save(ConcertSchedule.create(concert, LocalDateTime.now().plusDays(1)));
        });
    }

    private void initPointTxCommitted(Long userId, long amount) {
        transactionTemplate.executeWithoutResult(status -> {
            userPointJpaRepository.save(UserPoint.init(userId));
            userPointJpaRepository.findById(userId).orElseThrow().charge(amount);
        });
    }

    @Test
    void 홀드와_결제가_동시에_들어오면_홀드없이_시도한_결제는_SEAT_NOT_HELD로_실패한다() throws Exception {
        // given
        ConcertSchedule schedule = createScheduleTxCommitted();

        transactionTemplate.executeWithoutResult(status -> {
            seatRepository.save(ScheduleSeat.create(schedule, 1));
        });

        initPointTxCommitted(1L, 10_000L);

        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch startGate = new CountDownLatch(1);
        CountDownLatch doneGate = new CountDownLatch(2);

        ConcurrentLinkedQueue<TaskResult> results = new ConcurrentLinkedQueue<>();

        // 결제(홀드 없이 결제 시도 → 반드시 SEAT_NOT_HELD 여야 정상)
        pool.submit(() -> {
            try {
                startGate.await();
                results.add(runCatching(() ->
                        payUseCase.pay(new PayCommand(1L, schedule.getId(), 1, 500L))
                ));
            } catch (InterruptedException e) {
                results.add(new TaskResult(false, null, e));
            } finally {
                doneGate.countDown();
            }
        });

        // 홀드
        pool.submit(() -> {
            try {
                startGate.await();
                results.add(runCatching(() ->
                        holdSeatUseCase.hold(new HoldSeatCommand(1L, schedule.getId(), 1))
                ));
            } catch (InterruptedException e) {
                results.add(new TaskResult(false, null, e));
            } finally {
                doneGate.countDown();
            }
        });

        // when
        startGate.countDown();
        doneGate.await();
        pool.shutdown();

        // then
        assertThat(results).hasSize(2);

        // 실패는 결제여야 하고, 에러코드는 SEAT_NOT_HELD 여야 한다
        TaskResult fail = results.stream().filter(r -> !r.success()).findFirst().orElseThrow();
        assertThat(fail.errorCode()).isEqualTo(ErrorCode.SEAT_NOT_HELD);
    }

}
