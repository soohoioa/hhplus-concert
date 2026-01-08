package kr.hhplus.be.server.reservation.service;

import kr.hhplus.be.server.common.error.AppException;
import kr.hhplus.be.server.common.error.ErrorCode;
import kr.hhplus.be.server.concert.domain.Concert;
import kr.hhplus.be.server.concert.domain.ConcertSchedule;
import kr.hhplus.be.server.concert.repository.ConcertRepository;
import kr.hhplus.be.server.concert.repository.ConcertScheduleRepository;
import kr.hhplus.be.server.reservation.domain.ScheduleSeat;
import kr.hhplus.be.server.reservation.dto.SeatHoldRequest;
import kr.hhplus.be.server.reservation.dto.SeatHoldResponse;
import kr.hhplus.be.server.reservation.repository.ScheduleSeatRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class SeatHoldServiceTest { // 좌석 임시배정 성공/실패 + 만료 정책

    @Autowired
    ConcertRepository concertRepository;
    @Autowired
    ConcertScheduleRepository scheduleRepository;
    @Autowired
    ScheduleSeatRepository seatRepository;
    @Autowired
    SeatHoldService seatHoldService;

    // 공통 준비 헬퍼
    private ConcertSchedule createSchedule() {
        Concert concert = concertRepository.save(Concert.create("콘서트"));
        return scheduleRepository.save(ConcertSchedule.create(concert, LocalDateTime.now().plusDays(1)));
    }

    @Test
    void 좌석을_임시배정_할_수_있다() {
        ConcertSchedule schedule = createSchedule();
        seatRepository.save(ScheduleSeat.create(schedule, 1));

        SeatHoldResponse res = seatHoldService.holdSeat(new SeatHoldRequest(1L, schedule.getId(), 1));

        assertThat(res.getSeatNo()).isEqualTo(1);
    }

    @Test
    void 좌석정보가_없으면_임시배정_실패한다() {
        // given
        ConcertSchedule schedule = createSchedule();

        // when
        AppException ex = assertThrows(AppException.class,
                () -> seatHoldService.holdSeat(new SeatHoldRequest(1L, schedule.getId(), 1))
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
                () -> seatHoldService.holdSeat(new SeatHoldRequest(2L, schedule.getId(), 1))
        );

        assertEquals(ErrorCode.SEAT_HELD_BY_OTHER, ex.getErrorCode()); // 다른 사용자가 선점한 좌석입니다
    }

    @Test
    void 이미_예약이_확정된_좌석은_임시배정_실패한다() {
        ConcertSchedule schedule = createSchedule();
        ScheduleSeat seat = seatRepository.save(ScheduleSeat.create(schedule, 1));

        // RESERVED 만드는 방법 1: reserve()가 있으면 사용
        seat.reserve(1L, LocalDateTime.now());

        AppException ex = assertThrows(AppException.class,
                () -> seatHoldService.holdSeat(new SeatHoldRequest(2L, schedule.getId(), 1))
        );

        assertEquals(ErrorCode.SEAT_ALREADY_RESERVED, ex.getErrorCode()); // 이미 예약된 좌석입니다.
    }

    @Test
    void 임시배정이_만료된_좌석은_다른_사용자가_다시_임시배정_가능하다() {
        ConcertSchedule schedule = createSchedule();
        ScheduleSeat seat = seatRepository.save(ScheduleSeat.create(schedule, 1));

        seat.hold(1L, LocalDateTime.now().minusSeconds(1)); // 이미 만료

        SeatHoldResponse res = seatHoldService.holdSeat(new SeatHoldRequest(2L, schedule.getId(), 1));

        assertThat(res.getSeatNo()).isEqualTo(1);
    }

}