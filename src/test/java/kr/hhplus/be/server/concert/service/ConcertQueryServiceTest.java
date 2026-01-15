package kr.hhplus.be.server.concert.service;

import kr.hhplus.be.server.common.error.AppException;
import kr.hhplus.be.server.common.error.ErrorCode;
import kr.hhplus.be.server.concert.application.dto.GetAvailableSeatsQuery;
import kr.hhplus.be.server.concert.application.dto.GetAvailableSeatsResult;
import kr.hhplus.be.server.concert.application.dto.GetSchedulesQuery;
import kr.hhplus.be.server.concert.application.dto.ScheduleItem;
import kr.hhplus.be.server.concert.application.service.ConcertQueryUseCase;
import kr.hhplus.be.server.concert.domain.Concert;
import kr.hhplus.be.server.concert.domain.ConcertSchedule;
import kr.hhplus.be.server.concert.infrastructure.persistence.jpa.ConcertJpaRepository;
import kr.hhplus.be.server.concert.infrastructure.persistence.jpa.ConcertScheduleJpaRepository;
import kr.hhplus.be.server.reservation.domain.ScheduleSeat;
import kr.hhplus.be.server.reservation.infrastructure.persistence.jpa.ScheduleSeatRepository;
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
    ConcertJpaRepository concertJpaRepository;

    @Autowired
    ConcertScheduleJpaRepository concertScheduleJpaRepository;

    @Autowired
    ScheduleSeatRepository seatRepository;

    @Autowired
    ConcertQueryUseCase concertQueryUseCase;

    @Test
    void 예약가능한_날짜_목록을_조회한다() {
        // given
        Concert concert = concertJpaRepository.save(Concert.create("콘서트A"));

        concertScheduleJpaRepository.save(ConcertSchedule.create(concert, LocalDateTime.now().plusDays(1)));
        concertScheduleJpaRepository.save(ConcertSchedule.create(concert, LocalDateTime.now().plusDays(2)));

        // when
        List<ScheduleItem> result = concertQueryUseCase.getSchedules(new GetSchedulesQuery(concert.getId()));

        // then
        assertThat(result).hasSize(2);

        // 정렬 보장 확인(옵션)
        assertThat(result.get(0).getStartAt()).isBefore(result.get(1).getStartAt());
    }

    @Test
    void 예약가능한_좌석만_조회된다() {
        // given
        Concert concert = concertJpaRepository.save(Concert.create("콘서트A"));
        ConcertSchedule schedule =
                concertScheduleJpaRepository.save(ConcertSchedule.create(concert, LocalDateTime.now().plusDays(1)));

        // 좌석 생성
        for (int i = 1; i <= 50; i++) {
            seatRepository.save(ScheduleSeat.create(schedule, i));
        }

        // 일부 좌석 HOLD
        ScheduleSeat seat1 = seatRepository.findByScheduleIdAndSeatNo(schedule.getId(), 1).orElseThrow();
        seat1.hold(100L, LocalDateTime.now().plusMinutes(5));

        // when
        GetAvailableSeatsResult result = concertQueryUseCase.getAvailableSeats(
                new GetAvailableSeatsQuery(concert.getId(), schedule.getId())
        );

        // then
        assertThat(result.getAvailableSeatNos()).doesNotContain(1);
        assertThat(result.getAvailableSeatNos()).contains(2, 3, 50);
    }

    @Test
    void 예약가능한_날짜_조회시_콘서트가_없으면_에러발생() {
        // given
        Long notExistConcertId = 99999L;

        // when
        AppException ex = assertThrows(AppException.class,
                () -> concertQueryUseCase.getSchedules(new GetSchedulesQuery(notExistConcertId))
        );

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
                () -> concertQueryUseCase.getAvailableSeats(new GetAvailableSeatsQuery(notExistConcertId, anyScheduleId))
        );

        // then
        assertEquals(ErrorCode.CONCERT_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    void 예약가능한_좌석_조회시_스케줄이_없으면_에러발생() {
        // given
        Concert concert = concertJpaRepository.save(Concert.create("콘서트A"));
        Long notExistScheduleId = 99999L;

        // when
        AppException ex = assertThrows(AppException.class,
                () -> concertQueryUseCase.getAvailableSeats(new GetAvailableSeatsQuery(concert.getId(), notExistScheduleId))
        );

        // then
        assertEquals(ErrorCode.SCHEDULE_NOT_FOUND, ex.getErrorCode());
    }

}