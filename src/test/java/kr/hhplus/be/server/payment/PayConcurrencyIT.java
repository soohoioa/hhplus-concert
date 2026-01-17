package kr.hhplus.be.server.payment;

import jakarta.persistence.EntityManager;
import kr.hhplus.be.server.concert.domain.Concert;
import kr.hhplus.be.server.concert.domain.ConcertSchedule;
import kr.hhplus.be.server.payment.application.dto.PayCommand;
import kr.hhplus.be.server.payment.application.service.PayUseCase;
import kr.hhplus.be.server.point.application.dto.ChargePointCommand;
import kr.hhplus.be.server.point.application.service.ChargePointUseCase;
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
public class PayConcurrencyIT { // 결제(PayUseCase) 동시성 테스트 추가 (같은 좌석 결제 → 1명만 성공)

    @Autowired
    HoldSeatUseCase holdSeatUseCase;
    @Autowired
    PayUseCase payUseCase;
    @Autowired
    ChargePointUseCase chargePointUseCase;

    @Autowired
    ScheduleSeatRepository seatRepository;

    @Autowired
    PlatformTransactionManager txManager;
    @Autowired
    EntityManager entityManager;

    /**
     * 1. 먼저 한 유저가 hold로 좌석을 HELD 상태로 만든다.
     * 2. 그 다음 같은 유저가 동시에 pay를 여러 번 때린다.
     * 3. PayUseCaseImpl은 loadForUpdate(=PESSIMISTIC_WRITE)로 좌석을 잡고, 첫 트랜잭션이 RESERVED로 바꾸면 나머지는
     *    SEAT_ALREADY_RESERVED 또는 SEAT_NOT_HELD 등으로 실패하게 된다.
     * 4. 결과적으로 1번만 성공해야 함.
     *
     * 여기서 여러 유저가 동시에 결제는 의미가 조금 애매함
     * 결제는 holdUserId == userId 조건이 있어서 원래 다른 유저는 결제 자체가 실패하는 게 정상이라
     * 동시성 테스트는 동일 유저가 결제 API를 중복 호출했을 때 1번만 성공으로 진행
     */

    @Test
    void 동시에_같은좌석을_결제하면_1명만_성공한다() throws Exception {

        TransactionTemplate tx = new TransactionTemplate(txManager);

        Long userId = 1L;
        int seatNo = 1;
        long amount = 5_000L;

        // given: 커밋 보장 트랜잭션에서 데이터 생성 + 충전 + hold 완료
        Long scheduleId = tx.execute(status -> {
            Concert concert = Concert.create("test-concert");
            entityManager.persist(concert);

            ConcertSchedule schedule = ConcertSchedule.create(concert, LocalDateTime.now().plusDays(1));
            entityManager.persist(schedule);

            entityManager.persist(ScheduleSeat.create(schedule, seatNo));

            chargePointUseCase.charge(new ChargePointCommand(userId, 20_000L));

            entityManager.flush();

            holdSeatUseCase.hold(new HoldSeatCommand(userId, schedule.getId(), seatNo));

            entityManager.flush();
            return schedule.getId();
        });

        // sanity: hold 확인
        ScheduleSeat held = seatRepository.findByScheduleIdAndSeatNo(scheduleId, seatNo).orElseThrow();
        assertThat(held.getStatus()).isEqualTo(SeatStatus.HELD);
        assertThat(held.getHoldUserId()).isEqualTo(userId);

        // when: 동시에 결제 여러 번 시도
        int threadCount = 10;
        ExecutorService pool = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startGate = new CountDownLatch(1);
        CountDownLatch doneGate = new CountDownLatch(threadCount);

        ConcurrentLinkedQueue<Boolean> results = new ConcurrentLinkedQueue<>();

        for (int i = 0; i < threadCount; i++) {
            pool.submit(() -> {
                try {
                    startGate.await();

                    Boolean ok = tx.execute(status -> {
                        try {
                            payUseCase.pay(new PayCommand(userId, scheduleId, seatNo, amount));
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

        // then: 딱 1번만 성공
        long success = results.stream().filter(Boolean::booleanValue).count();
        assertThat(success).isEqualTo(1);

        ScheduleSeat latest = seatRepository.findByScheduleIdAndSeatNo(scheduleId, seatNo).orElseThrow();
        assertThat(latest.getStatus()).isEqualTo(SeatStatus.RESERVED);
        assertThat(latest.getReservedUserId()).isEqualTo(userId);
        assertThat(latest.getReservedAt()).isNotNull();
    }

}
