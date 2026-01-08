package kr.hhplus.be.server.reservation.repository;

import jakarta.persistence.LockModeType;
import kr.hhplus.be.server.reservation.domain.ScheduleSeat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ScheduleSeatRepository extends JpaRepository<ScheduleSeat, Long> {

    @Query("""
        select s
        from ScheduleSeat s
        where s.schedule.id = :scheduleId
          and (
              s.status = 'AVAILABLE'
              or (s.status = 'HELD' and s.holdExpiresAt < :now)
          )
        order by s.seatNo asc
    """)
    List<ScheduleSeat> findAvailableSeats(Long scheduleId, LocalDateTime now);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        select s
        from ScheduleSeat s
        where s.schedule.id = :scheduleId
          and s.seatNo = :seatNo
    """)
    Optional<ScheduleSeat> findByScheduleIdAndSeatNoForUpdate(Long scheduleId, Integer seatNo);

    @Query("""
        select s
    from ScheduleSeat s
    where s.status = 'HELD'
      and s.holdExpiresAt < :now
    """)
    List<ScheduleSeat> findExpiredHeldSeats(LocalDateTime now);

    Optional<ScheduleSeat> findByScheduleIdAndSeatNo(Long scheduleId, int seatNo);

}
