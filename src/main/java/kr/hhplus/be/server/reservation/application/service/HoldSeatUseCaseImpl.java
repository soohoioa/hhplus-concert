package kr.hhplus.be.server.reservation.application.service;

import kr.hhplus.be.server.common.error.AppException;
import kr.hhplus.be.server.common.error.ErrorCode;
import kr.hhplus.be.server.reservation.application.dto.HoldSeatCommand;
import kr.hhplus.be.server.reservation.application.dto.HoldSeatResult;
import kr.hhplus.be.server.reservation.port.out.LoadSeatForUpdatePort;
import kr.hhplus.be.server.reservation.domain.ScheduleSeat;
import kr.hhplus.be.server.reservation.domain.SeatStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional
public class HoldSeatUseCaseImpl implements HoldSeatUseCase {
    private static final Duration HOLD_DURATION = Duration.ofMinutes(5);

    private final LoadSeatForUpdatePort loadSeatForUpdatePort;

    @Override
    public HoldSeatResult hold(HoldSeatCommand holdSeatCommand) {
        validateSeatNo(holdSeatCommand.getSeatNo());

        LocalDateTime now = LocalDateTime.now();

//        ScheduleSeat seat = loadSeatForUpdatePort
//                .findByScheduleIdAndSeatNoForUpdate(holdSeatCommand.getScheduleId(), holdSeatCommand.getSeatNo())
//                .orElseThrow(() -> new AppException(ErrorCode.SEAT_NOT_FOUND));
        ScheduleSeat seat = loadSeatForUpdatePort
                .loadForUpdate(holdSeatCommand.getScheduleId(), holdSeatCommand.getSeatNo())
                .orElseThrow(() -> new AppException(ErrorCode.SEAT_NOT_FOUND));

        // 만료된 HELD는 즉시 해제(AVAILABLE 취급)
        if (seat.isHoldExpired(now)) {
            seat.releaseHold();
        }

        if (seat.getStatus() == SeatStatus.RESERVED) {
            throw new AppException(ErrorCode.SEAT_ALREADY_RESERVED);
        }
        if (seat.getStatus() == SeatStatus.HELD) {
            throw new AppException(ErrorCode.SEAT_HELD_BY_OTHER);
        }

        LocalDateTime expiresAt = now.plus(HOLD_DURATION);
        seat.hold(holdSeatCommand.getUserId(), expiresAt);

        return new HoldSeatResult(holdSeatCommand.getScheduleId(), holdSeatCommand.getSeatNo(), expiresAt);
    }

    private void validateSeatNo(Integer seatNo) {
        if (seatNo == null || seatNo < 1 || seatNo > 50) {
            throw new AppException(ErrorCode.SEAT_NO_OUT_OF_RANGE);
        }
    }
}
