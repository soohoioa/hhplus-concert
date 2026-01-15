package kr.hhplus.be.server.reservation.infrastructure.persistence.adapter;

import kr.hhplus.be.server.reservation.domain.ScheduleSeat;
import kr.hhplus.be.server.reservation.infrastructure.persistence.jpa.ScheduleSeatRepository;
import kr.hhplus.be.server.reservation.port.out.LoadSeatForUpdatePort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class LoadSeatForUpdateJpaAdapter implements LoadSeatForUpdatePort {

    private final ScheduleSeatRepository scheduleSeatRepository;

    @Override
    public Optional<ScheduleSeat> loadForUpdate(Long scheduleId, Integer seatNo) {
        return scheduleSeatRepository.findByScheduleIdAndSeatNoForUpdate(scheduleId, seatNo);
    }

}
