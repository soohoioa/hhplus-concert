package kr.hhplus.be.server.reservation;

import jakarta.persistence.EntityManager;
import kr.hhplus.be.server.concert.domain.Concert;
import kr.hhplus.be.server.concert.domain.ConcertSchedule;
import kr.hhplus.be.server.reservation.domain.ScheduleSeat;
import kr.hhplus.be.server.reservation.domain.SeatStatus;
import kr.hhplus.be.server.reservation.infrastructure.persistence.jpa.ScheduleSeatRepository;
import kr.hhplus.be.server.reservation.infrastructure.scheduler.SeatHoldExpireScheduler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
public class SeatHoldExpirySchedulerIT {

    @Autowired
    SeatHoldExpireScheduler scheduler;
    @Autowired
    ScheduleSeatRepository seatRepository;

    @Autowired
    PlatformTransactionManager txManager;
    @Autowired
    EntityManager entityManager;

    @Test
    void 만료된_hold는_스케줄러_실행으로_AVAILABLE로_해제된다() {
        TransactionTemplate tx = new TransactionTemplate(txManager);

        Long scheduleId = tx.execute(status -> {
            Concert concert = Concert.create("test-concert");
            entityManager.persist(concert);

            ConcertSchedule schedule = ConcertSchedule.create(concert, LocalDateTime.now().plusDays(1));
            entityManager.persist(schedule);

            ScheduleSeat seat = ScheduleSeat.create(schedule, 1);
            seat.hold(1L, LocalDateTime.now().minusMinutes(1)); // 만료된 HOLD
            entityManager.persist(seat);

            entityManager.flush();
            return schedule.getId();
        });

        // when: 스케줄러 메서드 직접 호출
        scheduler.releaseExpiredHolds();

        // then
        ScheduleSeat latest = seatRepository.findByScheduleIdAndSeatNo(scheduleId, 1).orElseThrow();
        assertThat(latest.getStatus()).isEqualTo(SeatStatus.AVAILABLE);
        assertThat(latest.getHoldUserId()).isNull();
        assertThat(latest.getHoldExpiresAt()).isNull();
    }
}
