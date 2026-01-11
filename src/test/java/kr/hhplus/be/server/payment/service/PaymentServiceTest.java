package kr.hhplus.be.server.payment.service;

import kr.hhplus.be.server.common.error.AppException;
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
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class PaymentServiceTest { // 결제 성공/실패(좌석 상태/포인트 부족/사용자 불일치 등)

    @Autowired PayUseCase payUseCase;
    @Autowired HoldSeatUseCase holdSeatUseCase;

    @Autowired UserPointJpaRepository userPointJpaRepository;
    @Autowired ScheduleSeatRepository seatRepository;

    @Autowired
    ConcertScheduleJpaRepository scheduleJpaRepository;
    @Autowired
    ConcertJpaRepository concertJpaRepository;

    private ConcertSchedule createSchedule() {
        Concert concert = concertJpaRepository.save(Concert.create("콘서트"));
        return scheduleJpaRepository.save(ConcertSchedule.create(concert, LocalDateTime.now().plusDays(1)));
    }

    private void initPoint(Long userId, long amount) {
        userPointJpaRepository.save(UserPoint.init(userId));
        userPointJpaRepository.findById(userId).orElseThrow().charge(amount);
    }

    @Test
    void 결제에_성공하면_좌석이_예약확정된다() {
        ConcertSchedule schedule = createSchedule();
        seatRepository.save(ScheduleSeat.create(schedule, 1));

        holdSeatUseCase.hold(new HoldSeatCommand(1L, schedule.getId(), 1));
        initPoint(1L, 1000L);

        payUseCase.pay(new PayCommand(1L, schedule.getId(), 1, 500L));

        ScheduleSeat seat = seatRepository.findByScheduleIdAndSeatNo(schedule.getId(), 1).orElseThrow();
        assertThat(seat.getStatus()).isEqualTo(SeatStatus.RESERVED);
    }

    @Test
    void 포인트가_부족하면_결제_실패한다() {
        ConcertSchedule schedule = createSchedule();
        seatRepository.save(ScheduleSeat.create(schedule, 1));

        holdSeatUseCase.hold(new HoldSeatCommand(1L, schedule.getId(), 1));
        initPoint(1L, 100L);

        AppException ex = assertThrows(AppException.class,
                () -> payUseCase.pay(new PayCommand(1L, schedule.getId(), 1, 500L))
        );

        assertEquals(ErrorCode.INSUFFICIENT_BALANCE, ex.getErrorCode());
    }

    @Test
    void 예약시간이_만료되면_결제_실패한다() {
        // given
        ConcertSchedule schedule = createSchedule();
        ScheduleSeat seat = seatRepository.save(ScheduleSeat.create(schedule, 1));

        // 홀드 상태 + 이미 만료된 시간
        seat.hold(1L, LocalDateTime.now().minusSeconds(1));
        initPoint(1L, 1000L);

        // when
        AppException ex = assertThrows(AppException.class,
                () -> payUseCase.pay(new PayCommand(1L, schedule.getId(), 1, 500L))
        );

        // then
        assertEquals(ErrorCode.HOLD_EXPIRED, ex.getErrorCode());
    }

    @Test
    void 이미_예약확정된_좌석은_결제_실패한다() {
        // given
        ConcertSchedule schedule = createSchedule();
        ScheduleSeat seat = seatRepository.save(ScheduleSeat.create(schedule, 1));
        seat.reserve(1L, LocalDateTime.now());

        initPoint(1L, 1000L);

        // when
        AppException ex = assertThrows(AppException.class,
                () -> payUseCase.pay(new PayCommand(1L, schedule.getId(), 1, 500L))
        );

        // then
        assertEquals(ErrorCode.SEAT_ALREADY_RESERVED, ex.getErrorCode());
    }

    @Test
    void 예약중인_사용자만_결제할_수_있다() {
        ConcertSchedule schedule = createSchedule();
        seatRepository.save(ScheduleSeat.create(schedule, 1));

        holdSeatUseCase.hold(new HoldSeatCommand(1L, schedule.getId(), 1));

        // userId=2가 결제 시도
        initPoint(2L, 1000L);

        AppException ex = assertThrows(AppException.class,
                () -> payUseCase.pay(new PayCommand(2L, schedule.getId(), 1, 500L))
        );

        assertEquals(ErrorCode.SEAT_HELD_BY_OTHER, ex.getErrorCode());
    }

}
