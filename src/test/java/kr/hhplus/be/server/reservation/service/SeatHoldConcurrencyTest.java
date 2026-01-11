package kr.hhplus.be.server.reservation.service;

import kr.hhplus.be.server.common.error.ErrorCode;
import kr.hhplus.be.server.concert.domain.Concert;
import kr.hhplus.be.server.concert.domain.ConcertSchedule;
import kr.hhplus.be.server.concert.infrastructure.persistence.jpa.ConcertJpaRepository;
import kr.hhplus.be.server.concert.infrastructure.persistence.jpa.ConcertScheduleJpaRepository;
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
public class SeatHoldConcurrencyTest extends ConcurrencyTestSupport {

    @Autowired
    ConcertJpaRepository concertJpaRepository;
    @Autowired
    ConcertScheduleJpaRepository scheduleJpaRepository;
    @Autowired ScheduleSeatRepository seatRepository;

    @Autowired
    HoldSeatUseCase holdSeatUseCase;

    @Autowired
    TransactionTemplate transactionTemplate;

    private ConcertSchedule createScheduleTxCommitted() {
        return transactionTemplate.execute(status -> {
            Concert concert = concertJpaRepository.save(Concert.create("콘서트"));
            return scheduleJpaRepository.save(ConcertSchedule.create(concert, LocalDateTime.now().plusDays(1)));
        });
    }

    @Test
    void 동시에_같은_좌석을_홀드하면_1명만_성공하고_나머지는_SEAT_HELD_BY_OTHER() throws Exception {
        // given
        ConcertSchedule schedule = createScheduleTxCommitted();

        transactionTemplate.executeWithoutResult(status -> {
            seatRepository.save(ScheduleSeat.create(schedule, 1));
        });

        int threadCount = 10;
        ExecutorService pool = Executors.newFixedThreadPool(threadCount);

        CountDownLatch startGate = new CountDownLatch(1);
        CountDownLatch doneGate = new CountDownLatch(threadCount);

        ConcurrentLinkedQueue<TaskResult> results = new ConcurrentLinkedQueue<>();

        for (int i = 0; i < threadCount; i++) {
            long userId = i + 1;
            pool.submit(() -> {
                try {
                    startGate.await();
                    results.add(runCatching(() ->
                            holdSeatUseCase.hold(new HoldSeatCommand(userId, schedule.getId(), 1))
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
        assertThat(successCount).isEqualTo(1);

        // AppException이 아닌 예외가 있으면 동시성/락 문제 가능성이 높으니 테스트 실패
        List<TaskResult> nonAppErrors = results.stream()
                .filter(r -> !r.success() && r.errorCode() == null)
                .toList();
        assertThat(nonAppErrors).isEmpty();

        // 실패는 전부 SEAT_HELD_BY_OTHER
        List<ErrorCode> errorCodes = results.stream()
                .filter(r -> !r.success())
                .map(TaskResult::errorCode)
                .toList();

        assertThat(errorCodes).allMatch(code -> code == ErrorCode.SEAT_HELD_BY_OTHER);

        // 상태 확인(선택)
        ScheduleSeat seat = seatRepository.findByScheduleIdAndSeatNo(schedule.getId(), 1).orElseThrow();
        assertThat(seat.getStatus()).isEqualTo(SeatStatus.HELD);
    }
}

// 동시성 테스트는 테스트 메서드에 @Transactional 을 붙이지 않는 걸 추천
// 이유 : 테스트 메서드 트랜잭션이 전체를 묶어버리면 스레드별 “별도 트랜잭션”이 깨져서 동시성 재현이 흔들릴 수 있음
// 대신 : 서비스 메서드(@Transactional) 가 트랜잭션 경계를 갖도록 두는게 안정적