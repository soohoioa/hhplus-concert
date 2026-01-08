package kr.hhplus.be.server.payment.service;

import kr.hhplus.be.server.common.error.AppException;
import kr.hhplus.be.server.common.error.ErrorCode;
import kr.hhplus.be.server.concert.domain.Concert;
import kr.hhplus.be.server.concert.domain.ConcertSchedule;
import kr.hhplus.be.server.concert.repository.ConcertRepository;
import kr.hhplus.be.server.concert.repository.ConcertScheduleRepository;
import kr.hhplus.be.server.payment.dto.PaymentRequest;
import kr.hhplus.be.server.point.domain.UserPoint;
import kr.hhplus.be.server.point.repository.UserPointRepository;
import kr.hhplus.be.server.reservation.domain.ScheduleSeat;
import kr.hhplus.be.server.reservation.domain.SeatStatus;
import kr.hhplus.be.server.reservation.dto.SeatHoldRequest;
import kr.hhplus.be.server.reservation.repository.ScheduleSeatRepository;
import kr.hhplus.be.server.reservation.service.SeatHoldService;
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

    @Autowired PaymentService paymentService;
    @Autowired SeatHoldService seatHoldService;

    @Autowired UserPointRepository userPointRepository;
    @Autowired ScheduleSeatRepository seatRepository;
    @Autowired ConcertScheduleRepository scheduleRepository;
    @Autowired ConcertRepository concertRepository;

    private ConcertSchedule createSchedule() {
        Concert concert = concertRepository.save(Concert.create("콘서트"));
        return scheduleRepository.save(ConcertSchedule.create(concert, LocalDateTime.now().plusDays(1)));
    }

    private void initPoint(Long userId, long amount) {
        userPointRepository.save(UserPoint.init(userId));
        userPointRepository.findById(userId).get().charge(amount);
    }

    @Test
    void 결제에_성공하면_좌석이_예약확정된다() {
        ConcertSchedule schedule = createSchedule();
        seatRepository.save(ScheduleSeat.create(schedule, 1));

        seatHoldService.holdSeat(new SeatHoldRequest(1L, schedule.getId(), 1));
        initPoint(1L, 1000L);

        paymentService.pay(new PaymentRequest(1L, schedule.getId(), 1, 500L));

        ScheduleSeat seat = seatRepository.findByScheduleIdAndSeatNoForUpdate(schedule.getId(), 1).get();
        assertThat(seat.getStatus()).isEqualTo(SeatStatus.RESERVED);
    }

    @Test
    void 포인트가_부족하면_결제_실패한다() {
        ConcertSchedule schedule = createSchedule();
        seatRepository.save(ScheduleSeat.create(schedule, 1));

        seatHoldService.holdSeat(new SeatHoldRequest(1L, schedule.getId(), 1));
        initPoint(1L, 100L);

        AppException ex = assertThrows(AppException.class,
                () -> paymentService.pay(new PaymentRequest(1L, schedule.getId(), 1, 500L))
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
                () -> paymentService.pay(new PaymentRequest(1L, schedule.getId(), 1, 500L))
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
                () -> paymentService.pay(new PaymentRequest(1L, schedule.getId(), 1, 500L))
        );

        // then
        assertEquals(ErrorCode.SEAT_ALREADY_RESERVED, ex.getErrorCode());
    }

    @Test
    void 예약중인_사용자만_결제할_수_있다() {
        // given
        ConcertSchedule schedule = createSchedule();
        seatRepository.save(ScheduleSeat.create(schedule, 1));

        seatHoldService.holdSeat(new SeatHoldRequest(1L, schedule.getId(), 1));
        initPoint(2L, 1000L);

        // when
        AppException ex = assertThrows(AppException.class,
                () -> paymentService.pay(new PaymentRequest(2L, schedule.getId(), 1, 500L))
        );

        // then
        assertThat(ex.getErrorCode()).isIn(ErrorCode.SEAT_HELD_BY_OTHER);
    }

}
