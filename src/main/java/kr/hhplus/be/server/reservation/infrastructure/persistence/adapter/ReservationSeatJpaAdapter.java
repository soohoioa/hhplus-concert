package kr.hhplus.be.server.reservation.infrastructure.persistence.adapter;

import kr.hhplus.be.server.reservation.port.out.LoadSeatForUpdatePort;
import kr.hhplus.be.server.reservation.domain.ScheduleSeat;
import kr.hhplus.be.server.reservation.infrastructure.persistence.jpa.ScheduleSeatRepository;
import kr.hhplus.be.server.reservation.port.out.ReleaseExpiredHoldsPort;
import kr.hhplus.be.server.reservation.port.out.ReservationSeatPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Repository
@RequiredArgsConstructor
public class ReservationSeatJpaAdapter implements ReleaseExpiredHoldsPort, ReservationSeatPort {

    private final ScheduleSeatRepository scheduleSeatRepository;

    @Override
    @Transactional
    public int releaseExpiredHolds(LocalDateTime now) {
        return scheduleSeatRepository.releaseExpiredHolds(now);
    }

    @Override
    public int tryHold(Long scheduleId, Integer seatNo, Long userId, LocalDateTime expiresAt, LocalDateTime now) {
        return scheduleSeatRepository.tryHold(scheduleId, seatNo, userId, expiresAt, now);
    }

}

