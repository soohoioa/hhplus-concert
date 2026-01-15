package kr.hhplus.be.server.reservation.port.out;

import java.time.LocalDateTime;

public interface ReservationSeatPort {
    int tryHold(Long scheduleId, Integer seatNo, Long userId, LocalDateTime expiresAt, LocalDateTime now);
}
