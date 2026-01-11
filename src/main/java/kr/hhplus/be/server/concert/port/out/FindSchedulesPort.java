package kr.hhplus.be.server.concert.port.out;

import kr.hhplus.be.server.concert.domain.ConcertSchedule;

import java.util.List;

public interface FindSchedulesPort {
    List<ConcertSchedule> findByConcertIdOrderByStartAtAsc(Long concertId);
}
