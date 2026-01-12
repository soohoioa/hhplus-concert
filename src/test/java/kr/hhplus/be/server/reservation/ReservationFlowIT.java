package kr.hhplus.be.server.reservation;

import jakarta.persistence.EntityManager;
import kr.hhplus.be.server.concert.domain.Concert;
import kr.hhplus.be.server.concert.domain.ConcertSchedule;
import kr.hhplus.be.server.payment.application.dto.PayCommand;
import kr.hhplus.be.server.payment.application.service.PayUseCase;
import kr.hhplus.be.server.point.application.dto.ChargePointCommand;
import kr.hhplus.be.server.point.application.service.ChargePointUseCase;
import kr.hhplus.be.server.reservation.application.dto.HoldSeatCommand;
import kr.hhplus.be.server.reservation.application.service.HoldSeatUseCase;
import kr.hhplus.be.server.reservation.application.service.ReservationTokenUseCase;
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
public class ReservationFlowIT { // 토큰 발급 -> 좌석 hold -> 결제 완료 흐름 테스트

    @Autowired
    ReservationTokenUseCase reservationTokenUseCase;
    @Autowired
    HoldSeatUseCase holdSeatUseCase;
    @Autowired
    PayUseCase payUseCase;

    @Autowired
    ScheduleSeatRepository scheduleSeatRepository;

    @Autowired
    ChargePointUseCase chargePointUseCase; // 있으면 사용(포인트 충전)
    @Autowired
    EntityManager entityManager;

    @Test
    @Transactional
    void 토큰발급_hold_결제완료_통합흐름() {

        Long userId = 1L;
        int seatNo = 1;

        // given
        ConcertSchedule schedule = createSchedule();
        entityManager.persist(ScheduleSeat.create(schedule, seatNo));

        chargePointUseCase.charge(new ChargePointCommand(userId, 10_000L));

        entityManager.flush();
        entityManager.clear();

        // when
        var token = reservationTokenUseCase.issue(userId);
        reservationTokenUseCase.validateActive(token.getToken());

        // hold
        holdSeatUseCase.hold(new HoldSeatCommand(userId, schedule.getId(), seatNo));

        // pay
        payUseCase.pay(new PayCommand(userId, schedule.getId(), seatNo, 5_000L));

        // consume token
        reservationTokenUseCase.consume(token.getToken());

        // then
        var latest = scheduleSeatRepository.findByScheduleIdAndSeatNo(schedule.getId(), seatNo).orElseThrow();
        assertThat(latest.getStatus()).isEqualTo(SeatStatus.RESERVED);
        assertThat(latest.getReservedUserId()).isEqualTo(userId);
        assertThat(latest.getReservedAt()).isNotNull();
    }

    private ConcertSchedule createSchedule() {
        Concert concert = Concert.create("test-concert");
        entityManager.persist(concert);

        ConcertSchedule schedule = ConcertSchedule.create(concert, LocalDateTime.now().plusDays(1));
        entityManager.persist(schedule);

        return schedule;
    }

}
