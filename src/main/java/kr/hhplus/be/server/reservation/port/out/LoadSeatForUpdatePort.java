package kr.hhplus.be.server.reservation.port.out;

import kr.hhplus.be.server.reservation.domain.ScheduleSeat;

import java.util.Optional;

public interface LoadSeatForUpdatePort {
    Optional<ScheduleSeat> loadForUpdate(Long scheduleId, Integer seatNo);
}
