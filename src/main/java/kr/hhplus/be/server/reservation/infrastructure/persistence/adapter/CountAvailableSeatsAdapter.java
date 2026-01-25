package kr.hhplus.be.server.reservation.infrastructure.persistence.adapter;

import kr.hhplus.be.server.reservation.domain.SeatStatus;
import kr.hhplus.be.server.reservation.infrastructure.persistence.jpa.ScheduleSeatRepository;
import kr.hhplus.be.server.reservation.port.out.CountAvailableSeatsPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CountAvailableSeatsAdapter implements CountAvailableSeatsPort {

    private final ScheduleSeatRepository scheduleSeatRepository;

    @Override
    public long countAvailableSeats(Long scheduleId) {
        return scheduleSeatRepository.countByScheduleIdAndStatus(scheduleId, SeatStatus.AVAILABLE);
    }
}
