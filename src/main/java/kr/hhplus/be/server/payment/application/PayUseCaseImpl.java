package kr.hhplus.be.server.payment.application;

import kr.hhplus.be.server.common.error.AppException;
import kr.hhplus.be.server.common.error.ErrorCode;
import kr.hhplus.be.server.payment.application.dto.PayCommand;
import kr.hhplus.be.server.payment.application.dto.PayResult;
import kr.hhplus.be.server.payment.application.port.CreatePaymentPort;
import kr.hhplus.be.server.payment.application.port.LoadUserPointPort;
import kr.hhplus.be.server.payment.domain.Payment;
import kr.hhplus.be.server.point.domain.UserPoint;
import kr.hhplus.be.server.reservation.application.port.LoadSeatForUpdatePort;
import kr.hhplus.be.server.reservation.domain.ScheduleSeat;
import kr.hhplus.be.server.reservation.domain.SeatStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Transactional
public class PayUseCaseImpl implements PayUseCase {

    private final LoadSeatForUpdatePort loadSeatForUpdatePort;
    private final LoadUserPointPort loadUserPointPort;
    private final CreatePaymentPort createPaymentPort;

    @Override
    public PayResult pay(PayCommand command) {
        validateSeatNo(command.getSeatNo());

        LocalDateTime now = LocalDateTime.now();

        // 1) 좌석 row 락 (결제 시점에도 최종 검증 필요)
        ScheduleSeat seat = loadSeatForUpdatePort
                .findByScheduleIdAndSeatNoForUpdate(command.getScheduleId(), command.getSeatNo())
                .orElseThrow(() -> new AppException(ErrorCode.SEAT_NOT_FOUND));

        // 2) 만료된 hold면 해제
        if (seat.isHoldExpired(now)) {
            seat.releaseHold();
        }

        // 3) hold 유효성 체크
        if (seat.getStatus() != SeatStatus.HELD) {
            throw new AppException(ErrorCode.SEAT_NOT_HELD);
        }
        if (!Objects.equals(seat.getHoldUserId(), command.getUserId())) {
            throw new AppException(ErrorCode.SEAT_HELD_BY_OTHER);
        }
        if (seat.getHoldExpiresAt() == null || seat.getHoldExpiresAt().isBefore(now)) {
            throw new AppException(ErrorCode.HOLD_EXPIRED);
        }

        // 4) 포인트 차감 (@Version 낙관 락)
        UserPoint point = loadUserPointPort.findByUserId(command.getUserId())
                .orElseThrow(() -> new AppException(ErrorCode.USER_POINT_NOT_FOUND));
        point.spend(command.getAmount());

        // 5) 좌석 RESERVED 확정
        seat.reserve(command.getUserId(), now);

        // 6) 결제내역 생성
        Payment payment = createPaymentPort.save(
                Payment.of(command.getUserId(), command.getScheduleId(), command.getSeatNo(), command.getAmount(), now)
        );

        return new PayResult(
                payment.getId(),
                command.getUserId(),
                command.getScheduleId(),
                command.getSeatNo(),
                command.getAmount(),
                now
        );
    }

    private void validateSeatNo(Integer seatNo) {
        if (seatNo == null || seatNo < 1 || seatNo > 50) {
            throw new AppException(ErrorCode.SEAT_NO_OUT_OF_RANGE);
        }
    }

}
