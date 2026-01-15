package kr.hhplus.be.server.reservation.application.service;

import kr.hhplus.be.server.common.error.AppException;
import kr.hhplus.be.server.common.error.ErrorCode;
import kr.hhplus.be.server.reservation.application.dto.HoldSeatCommand;
import kr.hhplus.be.server.reservation.application.dto.HoldSeatResult;
import kr.hhplus.be.server.reservation.port.out.ReservationSeatPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional
public class HoldSeatUseCaseImpl implements HoldSeatUseCase {

    private static final Duration HOLD_TTL = Duration.ofMinutes(5);

    private final ReservationSeatPort reservationSeatPort;

    @Override
    public HoldSeatResult hold(HoldSeatCommand command) {
        validateSeatNo(command.getSeatNo());

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiresAt = now.plus(HOLD_TTL);

        int updated = reservationSeatPort.tryHold(
                command.getScheduleId(),
                command.getSeatNo(),
                command.getUserId(),
                expiresAt,
                now
        );

        if (updated == 0) {
            // 실패 사유는 보통 이미 다른 사람이 HELD 거나 이미 RESERVED
            throw new AppException(ErrorCode.SEAT_HELD_BY_OTHER);
        }

        return new HoldSeatResult(command.getScheduleId(), command.getSeatNo(), expiresAt);
    }

    private void validateSeatNo(Integer seatNo) {
        if (seatNo == null || seatNo < 1 || seatNo > 50) {
            throw new AppException(ErrorCode.SEAT_NO_OUT_OF_RANGE);
        }
    }

}
