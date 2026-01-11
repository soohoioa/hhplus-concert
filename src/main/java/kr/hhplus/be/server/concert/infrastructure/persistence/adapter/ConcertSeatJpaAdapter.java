package kr.hhplus.be.server.concert.infrastructure.persistence.adapter;

import kr.hhplus.be.server.concert.port.out.FindAvailableSeatsPort;
import kr.hhplus.be.server.concert.port.out.SaveSeatsPort;
import kr.hhplus.be.server.reservation.domain.ScheduleSeat;
import kr.hhplus.be.server.reservation.infrastructure.persistence.jpa.ScheduleSeatRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class ConcertSeatJpaAdapter implements FindAvailableSeatsPort, SaveSeatsPort {

    private final ScheduleSeatRepository scheduleSeatRepository;

    @Override
    public List<Integer> findAvailableSeatNos(Long scheduleId, LocalDateTime now) {
        return scheduleSeatRepository.findAvailableSeats(scheduleId, now).stream()
                .map(ScheduleSeat::getSeatNo)
                .toList();
    }

    @Override
    public void saveAll(List<ScheduleSeat> seats) {
        scheduleSeatRepository.saveAll(seats);
    }
}
