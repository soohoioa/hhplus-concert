package kr.hhplus.be.server.payment;

import kr.hhplus.be.server.common.lock.DistributedLockExecutor;
import kr.hhplus.be.server.concert.domain.Concert;
import kr.hhplus.be.server.concert.domain.ConcertSchedule;
import kr.hhplus.be.server.concert.infrastructure.persistence.jpa.ConcertJpaRepository;
import kr.hhplus.be.server.concert.infrastructure.persistence.jpa.ConcertScheduleJpaRepository;
import kr.hhplus.be.server.payment.application.dto.PayCommand;
import kr.hhplus.be.server.payment.application.event.PaymentCompletedEvent;
import kr.hhplus.be.server.payment.application.service.PayUseCase;
import kr.hhplus.be.server.payment.port.out.SendReservationToDataPlatformPort;
import kr.hhplus.be.server.point.domain.UserPoint;
import kr.hhplus.be.server.point.infrastructure.persistence.jpa.UserPointJpaRepository;
import kr.hhplus.be.server.reservation.domain.ScheduleSeat;
import kr.hhplus.be.server.reservation.domain.SeatStatus;
import kr.hhplus.be.server.reservation.fixture.ScheduleSeatFixture;
import kr.hhplus.be.server.reservation.infrastructure.persistence.jpa.ScheduleSeatRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@SpringBootTest
public class PaymentDataPlatformEventIT {

    @Autowired
    PayUseCase payUseCase;

    @Autowired
    ScheduleSeatRepository scheduleSeatRepository;

    @Autowired
    UserPointJpaRepository userPointJpaRepository;

    @Autowired
    PlatformTransactionManager txManager;

    @Autowired
    ConcertScheduleJpaRepository concertScheduleRepository;

    @Autowired
    ConcertJpaRepository concertRepository;

    // AFTER_COMMIT 리스너가 호출하는 외부 포트
    @MockitoBean
    SendReservationToDataPlatformPort sendReservationToDataPlatformPort;

    // Redis 락 제거: supplier 그대로 실행
    @MockitoBean
    DistributedLockExecutor lockExecutor;


    @BeforeEach
    void setUp() {
        when(lockExecutor.executeWithLock(anyString(), any(Duration.class), any()))
                .thenAnswer(invocation -> {
                    @SuppressWarnings("unchecked")
                    Supplier<?> supplier = invocation.getArgument(2, Supplier.class);
                    return supplier.get();
                });

        // FK 때문에 삭제 순서 중요: seat -> schedule -> concert -> point
        scheduleSeatRepository.deleteAll();
        concertScheduleRepository.deleteAll();
        concertRepository.deleteAll();
        userPointJpaRepository.deleteAll();

        reset(sendReservationToDataPlatformPort);
    }

    @Test
    void 결제_성공_커밋되면_AFTER_COMMIT_이벤트가_실행되어_플랫폼전송이_1회_호출된다() {
        // given
        Long userId = 1L;
        Integer seatNo = 5;
        Long amount = 1_000L;

        givenUserPointWithBalance(userId, 10_000L);

        // schedule 먼저 만들고 그 id로 command 구성
        ConcertSchedule schedule = givenConcertSchedule();
        Long scheduleId = schedule.getId();

        givenHeldSeat(schedule, seatNo, userId, LocalDateTime.now().plusMinutes(5));

        PayCommand command = new PayCommand(userId, scheduleId, seatNo, amount);

        // when
        payUseCase.pay(command);

        // then
        ArgumentCaptor<PaymentCompletedEvent> captor =
                ArgumentCaptor.forClass(PaymentCompletedEvent.class);

        verify(sendReservationToDataPlatformPort, times(1)).send(captor.capture());

        PaymentCompletedEvent event = captor.getValue();
        assertThat(event.getUserId()).isEqualTo(userId);
        assertThat(event.getScheduleId()).isEqualTo(scheduleId);
        assertThat(event.getSeatNo()).isEqualTo(seatNo);
        assertThat(event.getAmount()).isEqualTo(amount);
        assertThat(event.getPaymentId()).isNotNull();
    }

    @Test
    void 결제_중_트랜잭션이_롤백되면_AFTER_COMMIT_이벤트는_실행되지_않는다() {
        // given
        Long userId = 1L;
        Integer seatNo = 5;
        Long amount = 1_000L;

        givenUserPointWithBalance(userId, 10_000L);

        ConcertSchedule schedule = givenConcertSchedule();
        Long scheduleId = schedule.getId();

        givenHeldSeat(schedule, seatNo, userId, LocalDateTime.now().plusMinutes(5));

        PayCommand command = new PayCommand(userId, scheduleId, seatNo, amount);

        TransactionTemplate tx = new TransactionTemplate(txManager);

        // when: 강제 롤백
        tx.execute(status -> {
            payUseCase.pay(command);
            status.setRollbackOnly();
            return null;
        });

        // then
        verify(sendReservationToDataPlatformPort, never()).send(any());
    }

    private void givenUserPointWithBalance(Long userId, Long balance) {
        UserPoint point = UserPoint.init(userId);
        point.charge(balance);
        userPointJpaRepository.save(point);
    }

    private ConcertSchedule givenConcertSchedule() {
        // 1) Concert 생성 (도메인 팩토리 사용)
        Concert concert = Concert.create("테스트 콘서트");
        concert = concertRepository.save(concert);

        // 2) ConcertSchedule 생성
        ConcertSchedule schedule =
                ConcertSchedule.create(concert, LocalDateTime.now().plusDays(1));

        return concertScheduleRepository.save(schedule);
    }

    private void givenHeldSeat(
            ConcertSchedule schedule,
            Integer seatNo,
            Long holdUserId,
            LocalDateTime expiresAt
    ) {
        // fixture에서 이미 HELD 상태로 만들어줌
        ScheduleSeat seat = ScheduleSeatFixture.held(schedule, seatNo, holdUserId, expiresAt);
        scheduleSeatRepository.save(seat);

        assertThat(seat.getStatus()).isEqualTo(SeatStatus.HELD);
    }

}
