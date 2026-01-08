package kr.hhplus.be.server.concert.service;

import kr.hhplus.be.server.common.error.AppException;
import kr.hhplus.be.server.common.error.ErrorCode;
import kr.hhplus.be.server.concert.domain.Concert;
import kr.hhplus.be.server.concert.domain.ConcertSchedule;
import kr.hhplus.be.server.concert.dto.AvailableSeatsResponse;
import kr.hhplus.be.server.concert.dto.ScheduleResponse;
import kr.hhplus.be.server.concert.repository.ConcertRepository;
import kr.hhplus.be.server.concert.repository.ConcertScheduleRepository;
import kr.hhplus.be.server.reservation.domain.ScheduleSeat;
import kr.hhplus.be.server.reservation.repository.ScheduleSeatRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class ConcertQueryServiceTest {

    @Autowired
    ConcertRepository concertRepository;
    @Autowired
    ConcertScheduleRepository scheduleRepository;
    @Autowired
    ScheduleSeatRepository seatRepository;
    @Autowired
    ConcertQueryService concertQueryService;

    @Test
    void 예약가능한_날짜_목록을_조회한다() {
        // given
        Concert concert = concertRepository.save(Concert.create("콘서트A"));

        ConcertSchedule concertSchedule1 = scheduleRepository.save(ConcertSchedule.create(concert, LocalDateTime.now().plusDays(1)));
        ConcertSchedule concertSchedule2 = scheduleRepository.save(ConcertSchedule.create(concert, LocalDateTime.now().plusDays(2)));

        // when
        List<ScheduleResponse> result = concertQueryService.getSchedules(concert.getId());

        // then
        assertThat(result).hasSize(2);
    }

    @Test
    void 예약가능한_좌석만_조회된다() {
        // given
        Concert concert = concertRepository.save(Concert.create("콘서트A"));
        ConcertSchedule schedule = scheduleRepository.save(ConcertSchedule.create(concert, LocalDateTime.now().plusDays(1)));

        // 좌석 생성
        for (int i = 1; i <= 50; i++) {
            seatRepository.save(ScheduleSeat.create(schedule, i));
        }

        // 일부 좌석 HOLD
        ScheduleSeat seat1 = seatRepository.findByScheduleIdAndSeatNoForUpdate(schedule.getId(), 1).get();
        seat1.hold(100L, LocalDateTime.now().plusMinutes(5));

        // when
        AvailableSeatsResponse result = concertQueryService.getAvailableSeats(concert.getId(), schedule.getId());

        // then
        assertThat(result.getAvailableSeatNos()).doesNotContain(1);
        assertThat(result.getAvailableSeatNos()).contains(2, 3, 50);
    }

    @Test
    void 예약가능한_날짜_조회시_콘서트가_없으면_에러발생() {
        // given
        Long notExistConcertId = 99999L;

        // when
        AppException ex = assertThrows(AppException.class, () -> concertQueryService.getSchedules(notExistConcertId));

        // then
        assertEquals(ErrorCode.CONCERT_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    void 예약가능한_좌석_조회시_콘서트가_없으면_에러발생() {
        // given
        Long notExistConcertId = 99999L;
        Long anyScheduleId = 1L;

        // when
        AppException ex = assertThrows(AppException.class,
                () -> concertQueryService.getAvailableSeats(notExistConcertId, anyScheduleId)
        );

        // then
        assertEquals(ErrorCode.CONCERT_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    void 예약가능한_좌석_조회시_스케줄이_없으면_에러발생() {
        // given
        Concert concert = concertRepository.save(Concert.create("콘서트A"));
        Long notExistScheduleId = 99999L;

        // when
        AppException ex = assertThrows(AppException.class,
                () -> concertQueryService.getAvailableSeats(concert.getId(), notExistScheduleId)
        );

        // then
        assertEquals(ErrorCode.SCHEDULE_NOT_FOUND, ex.getErrorCode());
    }

//    @Test
//    void 예약가능_좌석조회시_다른_콘서트의_스케줄이면_에러발생() {
//        // given
//        Concert concertA = concertRepository.save(Concert.create("콘서트A"));
//        Concert concertB = concertRepository.save(Concert.create("콘서트B"));
//
//        ConcertSchedule scheduleOfB =
//                scheduleRepository.save(ConcertSchedule.create(concertB, LocalDateTime.now().plusDays(1)));
//
//        // when
//        AppException ex = assertThrows(AppException.class,
//                () -> concertQueryService.getAvailableSeats(concertA.getId(), scheduleOfB.getId())
//        );
//
//        // then (정책에 맞춰 택1)
//        assertThat(ex.getErrorCode()).isIn(ErrorCode.SCHEDULE_NOT_FOUND, ErrorCode.INVALID_REQUEST);
//    }

}