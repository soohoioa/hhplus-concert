package kr.hhplus.be.server.reservation.application.service;

import kr.hhplus.be.server.common.error.AppException;
import kr.hhplus.be.server.common.error.ErrorCode;
import kr.hhplus.be.server.common.lock.DistributedLockExecutor;
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

    // 분산락 TTL (임계구역 보호용)
    private static final Duration LOCK_TTL = Duration.ofSeconds(5);

    private final ReservationSeatPort reservationSeatPort;
    private final DistributedLockExecutor lockExecutor;

    @Override
    public HoldSeatResult hold(HoldSeatCommand command) {
        validateSeatNo(command.getSeatNo());

        String lockKey = buildSeatLockKey(command.getScheduleId(), command.getSeatNo());

        // 락 범위: 검증/홀드/저장(tryHold)까지 한 번에
        return lockExecutor.executeWithLock(lockKey, LOCK_TTL, () -> {
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
                throw new AppException(ErrorCode.SEAT_HELD_BY_OTHER);
            }

            return new HoldSeatResult(command.getScheduleId(), command.getSeatNo(), expiresAt);
        });
    }

    private String buildSeatLockKey(Long scheduleId, Integer seatNo) {
        return "lock:seat:" + scheduleId + ":" + seatNo;
    }

    private void validateSeatNo(Integer seatNo) {
        if (seatNo == null || seatNo < 1 || seatNo > 50) {
            throw new AppException(ErrorCode.SEAT_NO_OUT_OF_RANGE);
        }
    }

}
