package kr.hhplus.be.server.reservation.application.port;

import kr.hhplus.be.server.reservation.domain.ScheduleSeat;

import java.util.Optional;

public interface LoadSeatForUpdatePort {
    Optional<ScheduleSeat> findByScheduleIdAndSeatNoForUpdate(Long scheduleId, Integer seatNo);
}
