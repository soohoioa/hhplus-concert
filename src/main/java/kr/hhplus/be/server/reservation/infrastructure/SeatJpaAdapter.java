package kr.hhplus.be.server.reservation.infrastructure;

import kr.hhplus.be.server.reservation.application.port.LoadSeatForUpdatePort;
import kr.hhplus.be.server.reservation.domain.ScheduleSeat;
import kr.hhplus.be.server.reservation.repository.ScheduleSeatRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class SeatJpaAdapter implements LoadSeatForUpdatePort {

    private final ScheduleSeatRepository seatRepository;

    @Override
    public Optional<ScheduleSeat> findByScheduleIdAndSeatNoForUpdate(Long scheduleId, Integer seatNo) {
        return seatRepository.findByScheduleIdAndSeatNoForUpdate(scheduleId, seatNo);
    }
}

