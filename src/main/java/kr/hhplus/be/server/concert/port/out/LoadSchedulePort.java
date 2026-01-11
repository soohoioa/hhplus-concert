package kr.hhplus.be.server.concert.port.out;

import kr.hhplus.be.server.concert.domain.ConcertSchedule;

import java.util.Optional;

public interface LoadSchedulePort {
    Optional<ConcertSchedule> findByIdAndConcertId(Long scheduleId, Long concertId);
}
