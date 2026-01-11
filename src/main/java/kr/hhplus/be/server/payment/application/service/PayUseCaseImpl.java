package kr.hhplus.be.server.payment.application.service;

import kr.hhplus.be.server.common.error.AppException;
import kr.hhplus.be.server.common.error.ErrorCode;
import kr.hhplus.be.server.payment.application.dto.PayCommand;
import kr.hhplus.be.server.payment.application.dto.PayResult;
import kr.hhplus.be.server.payment.port.out.CreatePaymentPort;
import kr.hhplus.be.server.payment.domain.Payment;
import kr.hhplus.be.server.point.application.service.SpendPointUseCase;
import kr.hhplus.be.server.point.domain.UserPoint;
import kr.hhplus.be.server.reservation.port.out.LoadSeatForUpdatePort;
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
    private final SpendPointUseCase spendPointUseCase;
    private final CreatePaymentPort createPaymentPort;

    @Override
    public PayResult pay(PayCommand command) {
        validateSeatNo(command.getSeatNo());

        LocalDateTime now = LocalDateTime.now();

        ScheduleSeat seat = loadSeatForUpdatePort
                .loadForUpdate(command.getScheduleId(), command.getSeatNo())
                .orElseThrow(() -> new AppException(ErrorCode.SEAT_NOT_FOUND));

        // 상태/만료 검증 (결제는 "만료면 실패"가 일관됨)
        if (seat.getStatus() == SeatStatus.RESERVED) {
            throw new AppException(ErrorCode.SEAT_ALREADY_RESERVED);
        }
        if (seat.getStatus() != SeatStatus.HELD) {
            throw new AppException(ErrorCode.SEAT_NOT_HELD);
        }
        if (!Objects.equals(seat.getHoldUserId(), command.getUserId())) {
            throw new AppException(ErrorCode.SEAT_HELD_BY_OTHER);
        }
        LocalDateTime expiresAt = seat.getHoldExpiresAt();
        if (expiresAt == null || !expiresAt.isAfter(now)) {
            throw new AppException(ErrorCode.HOLD_EXPIRED);
        }

//        UserPoint point = loadUserPointPort.findByUserId(command.getUserId())
//                .orElseThrow(() -> new AppException(ErrorCode.USER_POINT_NOT_FOUND));
//        point.spend(command.getAmount());
//        saveUserPointPort.save(point);
        spendPointUseCase.spend(command.getUserId(), command.getAmount());

        seat.reserve(command.getUserId(), now);

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
