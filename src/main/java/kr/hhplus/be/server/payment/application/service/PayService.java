package kr.hhplus.be.server.payment.application.service;

import kr.hhplus.be.server.common.error.AppException;
import kr.hhplus.be.server.common.error.ErrorCode;
import kr.hhplus.be.server.payment.application.dto.PayCommand;
import kr.hhplus.be.server.payment.application.dto.PayResult;
import kr.hhplus.be.server.payment.domain.Payment;
import kr.hhplus.be.server.payment.port.out.CreatePaymentPort;
import kr.hhplus.be.server.point.application.service.SpendPointUseCase;
import kr.hhplus.be.server.reservation.domain.ScheduleSeat;
import kr.hhplus.be.server.reservation.domain.SeatStatus;
import kr.hhplus.be.server.reservation.port.out.LoadSeatForUpdatePort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class PayService {

    private final LoadSeatForUpdatePort loadSeatForUpdatePort;
    private final SpendPointUseCase spendPointUseCase;
    private final CreatePaymentPort createPaymentPort;

    @Transactional
    public PayResult pay(PayCommand command) {

        LocalDateTime now = LocalDateTime.now();

        ScheduleSeat seat = loadSeatForUpdatePort
                .loadForUpdate(command.getScheduleId(), command.getSeatNo())
                .orElseThrow(() -> new AppException(ErrorCode.SEAT_NOT_FOUND));

        if (seat.getStatus() == SeatStatus.RESERVED) {
            throw new AppException(ErrorCode.SEAT_ALREADY_RESERVED);
        }
        if (seat.getStatus() != SeatStatus.HELD) {
            throw new AppException(ErrorCode.SEAT_NOT_HELD);
        }
        if (!seat.getHoldUserId().equals(command.getUserId())) {
            throw new AppException(ErrorCode.PAYMENT_USER_MISMATCH);
        }
        if (!seat.getHoldExpiresAt().isAfter(now)) {
            throw new AppException(ErrorCode.HOLD_EXPIRED);
        }

        // 포인트 차감 (이미 동시성 제어 완료)
        spendPointUseCase.spend(command.getUserId(), command.getAmount());

        seat.reserve(command.getUserId(), now);

        Payment payment = createPaymentPort.save(
                Payment.of(
                        command.getUserId(),
                        command.getScheduleId(),
                        command.getSeatNo(),
                        command.getAmount(),
                        now
                )
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
}
