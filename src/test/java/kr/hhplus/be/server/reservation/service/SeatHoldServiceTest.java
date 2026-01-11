package kr.hhplus.be.server.reservation.service;

import kr.hhplus.be.server.common.error.AppException;
import kr.hhplus.be.server.common.error.ErrorCode;
import kr.hhplus.be.server.concert.domain.Concert;
import kr.hhplus.be.server.concert.domain.ConcertSchedule;
import kr.hhplus.be.server.concert.infrastructure.persistence.jpa.ConcertJpaRepository;
import kr.hhplus.be.server.concert.infrastructure.persistence.jpa.ConcertScheduleJpaRepository;
import kr.hhplus.be.server.reservation.application.dto.HoldSeatCommand;
import kr.hhplus.be.server.reservation.application.dto.HoldSeatResult;
import kr.hhplus.be.server.reservation.application.service.HoldSeatUseCase;
import kr.hhplus.be.server.reservation.domain.ScheduleSeat;
import kr.hhplus.be.server.reservation.infrastructure.persistence.jpa.ScheduleSeatRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class SeatHoldServiceTest { // 좌석 임시배정 성공/실패 + 만료 정책

    @Autowired
    ConcertJpaRepository concertJpaRepository;
    @Autowired
    ConcertScheduleJpaRepository scheduleJpaRepository;
    @Autowired
    ScheduleSeatRepository seatRepository;

    @Autowired
    HoldSeatUseCase holdSeatUseCase;

    private ConcertSchedule createSchedule() {
        Concert concert = concertJpaRepository.save(Concert.create("콘서트"));
        return scheduleJpaRepository.save(ConcertSchedule.create(concert, LocalDateTime.now().plusDays(1)));
    }

    @Test
    void 좌석을_임시배정_할_수_있다() {
        ConcertSchedule schedule = createSchedule();
        seatRepository.save(ScheduleSeat.create(schedule, 1));

        HoldSeatResult holdSeatResult = holdSeatUseCase.hold(new HoldSeatCommand(1L, schedule.getId(), 1));

        assertThat(holdSeatResult.getSeatNo()).isEqualTo(1);
        assertThat(holdSeatResult.getScheduleId()).isEqualTo(schedule.getId());
        assertThat(holdSeatResult.getHoldExpiresAt()).isNotNull();
    }

    @Test
    void 좌석정보가_없으면_임시배정_실패한다() {
        // given
        ConcertSchedule schedule = createSchedule();

        // when
        AppException ex = assertThrows(AppException.class,
                () -> holdSeatUseCase.hold(new HoldSeatCommand(1L, schedule.getId(), 1))
        );

        // then
        assertEquals(ErrorCode.SEAT_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    void 다른_사용자가_임시배정중인_좌석은_실패한다() {
        ConcertSchedule schedule = createSchedule();
        ScheduleSeat seat = seatRepository.save(ScheduleSeat.create(schedule, 1));
        seat.hold(1L, LocalDateTime.now().plusMinutes(5));

        AppException ex = assertThrows(AppException.class,
                () -> holdSeatUseCase.hold(new HoldSeatCommand(2L, schedule.getId(), 1))
        );

        assertEquals(ErrorCode.SEAT_HELD_BY_OTHER, ex.getErrorCode()); // 다른 사용자가 선점한 좌석입니다
    }

    @Test
    void 이미_예약이_확정된_좌석은_임시배정_실패한다() {
        ConcertSchedule schedule = createSchedule();
        ScheduleSeat scheduleSeat = seatRepository.save(ScheduleSeat.create(schedule, 1));

        // RESERVED 만드는 방법 1: reserve()가 있으면 사용
        scheduleSeat.reserve(1L, LocalDateTime.now());

        AppException ex = assertThrows(AppException.class,
                () -> holdSeatUseCase.hold(new HoldSeatCommand(2L, schedule.getId(), 1))
        );

        assertEquals(ErrorCode.SEAT_ALREADY_RESERVED, ex.getErrorCode()); // 이미 예약된 좌석입니다.
    }

    @Test
    void 임시배정이_만료된_좌석은_다른_사용자가_다시_임시배정_가능하다() {
        ConcertSchedule schedule = createSchedule();
        ScheduleSeat scheduleSeat = seatRepository.save(ScheduleSeat.create(schedule, 1));

        scheduleSeat.hold(1L, LocalDateTime.now().minusSeconds(1)); // 이미 만료

        HoldSeatResult holdSeatResult = holdSeatUseCase.hold(new HoldSeatCommand(2L, schedule.getId(), 1));

        assertThat(holdSeatResult.getSeatNo()).isEqualTo(1);
    }

}