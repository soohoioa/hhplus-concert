package kr.hhplus.be.server.reservation.infrastructure.persistence.adapter;

import kr.hhplus.be.server.reservation.port.out.LoadSeatForUpdatePort;
import kr.hhplus.be.server.reservation.domain.ScheduleSeat;
import kr.hhplus.be.server.reservation.infrastructure.persistence.jpa.ScheduleSeatRepository;
import kr.hhplus.be.server.reservation.port.out.ReleaseExpiredHoldsPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class ReservationSeatJpaAdapter implements LoadSeatForUpdatePort, ReleaseExpiredHoldsPort {

    private final ScheduleSeatRepository seatRepository;

    @Override
    public Optional<ScheduleSeat> loadForUpdate(Long scheduleId, Integer seatNo) {
        return seatRepository.findByScheduleIdAndSeatNoForUpdate(scheduleId, seatNo);
    }

    @Override
    @Transactional
    public int releaseExpiredHolds(LocalDateTime now) {
        return seatRepository.releaseExpiredHolds(now);
    }

//    @Override
//    public Optional<ScheduleSeat> findByScheduleIdAndSeatNoForUpdate(Long scheduleId, Integer seatNo) {
//        return seatRepository.findByScheduleIdAndSeatNoForUpdate(scheduleId, seatNo);
//    }
}

