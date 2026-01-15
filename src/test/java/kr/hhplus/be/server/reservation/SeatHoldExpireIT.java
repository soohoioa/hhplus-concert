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
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
public class SeatHoldExpireIT { // 만료 시간 도래 후 좌석이 다시 예약 가능한지 확인

    @Autowired
    HoldSeatUseCase holdSeatUseCase;
    @Autowired
    ScheduleSeatRepository seatRepository;
    @Autowired
    EntityManager entityManager;

    @Test
    @Transactional
    void HOLD_만료되면_다른유저가_재HOLD_가능() {
        // given
        ConcertSchedule schedule = createSchedule();
        entityManager.persist(ScheduleSeat.create(schedule, 1));
        entityManager.flush();
        entityManager.clear();

        Long scheduleId = schedule.getId();

        // user1 hold
        holdSeatUseCase.hold(new HoldSeatCommand(1L, scheduleId, 1));
        entityManager.flush();
        entityManager.clear();

        // 강제 만료: hold_expires_at을 과거로
        entityManager.createQuery("""
            update ScheduleSeat s
               set s.holdExpiresAt = :past
             where s.schedule.id = :scheduleId
               and s.seatNo = :seatNo
        """)
                .setParameter("past", LocalDateTime.now().minusMinutes(1))
                .setParameter("scheduleId", scheduleId)
                .setParameter("seatNo", 1)
                .executeUpdate();

        entityManager.flush();
        entityManager.clear();

        // when: user2 hold 재시도
        holdSeatUseCase.hold(new HoldSeatCommand(2L, scheduleId, 1));

        // then
        ScheduleSeat latest = seatRepository.findByScheduleIdAndSeatNo(scheduleId, 1).orElseThrow();
        assertThat(latest.getStatus()).isEqualTo(SeatStatus.HELD);
        assertThat(latest.getHoldUserId()).isEqualTo(2L);
        assertThat(latest.getHoldExpiresAt()).isNotNull();
    }

    private ConcertSchedule createSchedule() {
        Concert concert = Concert.create("test-concert");
        entityManager.persist(concert);

        ConcertSchedule schedule = ConcertSchedule.create(concert, LocalDateTime.now().plusDays(1));
        entityManager.persist(schedule);

        return schedule;
    }
}
