package kr.hhplus.be.server.reservation.service;

import kr.hhplus.be.server.common.error.AppException;
import kr.hhplus.be.server.common.error.ErrorCode;
import kr.hhplus.be.server.reservation.domain.ScheduleSeat;
import kr.hhplus.be.server.reservation.domain.SeatStatus;
import kr.hhplus.be.server.reservation.dto.SeatHoldRequest;
import kr.hhplus.be.server.reservation.dto.SeatHoldResponse;
import kr.hhplus.be.server.reservation.repository.ScheduleSeatRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional
public class SeatHoldService {

    private static final Duration HOLD_DURATION = Duration.ofMinutes(5);

    private final ScheduleSeatRepository seatRepository;

    public SeatHoldResponse holdSeat(SeatHoldRequest sendRequest) {
        validateSeatNo(sendRequest.getSeatNo());

        LocalDateTime now = LocalDateTime.now();
        ScheduleSeat seat = seatRepository.findByScheduleIdAndSeatNoForUpdate(sendRequest.getScheduleId(), sendRequest.getSeatNo())
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
        seat.hold(sendRequest.getUserId(), expiresAt);

        return new SeatHoldResponse(sendRequest.getScheduleId(), sendRequest.getSeatNo(), expiresAt);
    }

    private void validateSeatNo(Integer seatNo) {
        if (seatNo == null || seatNo < 1 || seatNo > 50) {
            throw new AppException(ErrorCode.SEAT_NO_OUT_OF_RANGE);
        }
    }

}
