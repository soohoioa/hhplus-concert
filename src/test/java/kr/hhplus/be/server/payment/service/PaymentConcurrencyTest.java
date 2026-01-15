package kr.hhplus.be.server.payment.service;

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
import kr.hhplus.be.server.reservation.domain.SeatStatus;
import kr.hhplus.be.server.reservation.infrastructure.persistence.jpa.ScheduleSeatRepository;
import kr.hhplus.be.server.support.ConcurrencyTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
public class PaymentConcurrencyTest extends ConcurrencyTestSupport {

    @Autowired PayUseCase payUseCase;
    @Autowired HoldSeatUseCase holdSeatUseCase;

    @Autowired UserPointJpaRepository userPointJpaRepository;
    @Autowired ScheduleSeatRepository seatRepository;

    @Autowired
    ConcertScheduleJpaRepository scheduleJpaRepository;
    @Autowired
    ConcertJpaRepository concertJpaRepository;

    @Autowired TransactionTemplate transactionTemplate;

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
    void 동시에_같은_좌석을_결제하면_1명만_성공한다() throws Exception {
        // given (준비 단계는 커밋까지 보장)
        ConcertSchedule schedule = createScheduleTxCommitted();

        transactionTemplate.executeWithoutResult(status -> {
            seatRepository.save(ScheduleSeat.create(schedule, 1));
        });

        transactionTemplate.executeWithoutResult(status -> {
            holdSeatUseCase.hold(new HoldSeatCommand(1L, schedule.getId(), 1));
        });

        initPointTxCommitted(1L, 10_000L);

        int threadCount = 10;
        ExecutorService pool = Executors.newFixedThreadPool(threadCount);

        CountDownLatch startGate = new CountDownLatch(1);
        CountDownLatch doneGate = new CountDownLatch(threadCount);

        ConcurrentLinkedQueue<TaskResult> results = new ConcurrentLinkedQueue<>();

        for (int i = 0; i < threadCount; i++) {
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
        }

        // when
        startGate.countDown();
        doneGate.await();
        pool.shutdown();

        // then
        long successCount = results.stream().filter(TaskResult::success).count();
        if (successCount == 0) {
            List<ErrorCode> codes = results.stream()
                    .filter(r -> !r.success())
                    .map(TaskResult::errorCode)
                    .toList();
            throw new AssertionError("모든 결제가 실패했습니다. errorCodes=" + codes);
        }
        assertThat(successCount).isEqualTo(1);

        // AppException이 아닌 예외는 동시성 문제 가능성 → 실패
        List<TaskResult> nonAppErrors = results.stream()
                .filter(r -> !r.success() && r.errorCode() == null)
                .toList();
        assertThat(nonAppErrors).isEmpty();

        // 실패는 정책상 RESERVED/NOT_HELD 중 하나로 수렴 (정책 확정 후 하나로 고정 추천)
        List<ErrorCode> errorCodes = results.stream()
                .filter(r -> !r.success())
                .map(TaskResult::errorCode)
                .toList();

        assertThat(errorCodes).allMatch(code ->
                code == ErrorCode.SEAT_ALREADY_RESERVED || code == ErrorCode.SEAT_NOT_HELD
        );

        // 좌석 상태 확인(마지막 조회는 락 없이)
        ScheduleSeat seat = seatRepository.findByScheduleIdAndSeatNo(schedule.getId(), 1).orElseThrow();
        assertThat(seat.getStatus()).isEqualTo(SeatStatus.RESERVED);
    }
}
