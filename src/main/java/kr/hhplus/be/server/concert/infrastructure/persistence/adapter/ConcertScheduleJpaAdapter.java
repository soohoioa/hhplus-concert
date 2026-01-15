package kr.hhplus.be.server.concert.infrastructure.persistence.adapter;

import kr.hhplus.be.server.concert.domain.ConcertSchedule;
import kr.hhplus.be.server.concert.infrastructure.persistence.jpa.ConcertScheduleJpaRepository;
import kr.hhplus.be.server.concert.port.out.FindSchedulesPort;
import kr.hhplus.be.server.concert.port.out.LoadSchedulePort;
import kr.hhplus.be.server.concert.port.out.SaveSchedulePort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class ConcertScheduleJpaAdapter implements FindSchedulesPort, LoadSchedulePort, SaveSchedulePort {

    private final ConcertScheduleJpaRepository concertScheduleJpaRepository;

    @Override
    public List<ConcertSchedule> findByConcertIdOrderByStartAtAsc(Long concertId) {
        return concertScheduleJpaRepository.findByConcertIdOrderByStartAtAsc(concertId);
    }

    @Override
    public Optional<ConcertSchedule> findByIdAndConcertId(Long scheduleId, Long concertId) {
        return concertScheduleJpaRepository.findByIdAndConcertId(scheduleId, concertId);
    }

    @Override
    public ConcertSchedule save(ConcertSchedule schedule) {
        return concertScheduleJpaRepository.save(schedule);
    }
}
