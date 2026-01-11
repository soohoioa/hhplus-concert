package kr.hhplus.be.server.reservation.infrastructure.persistence.jpa;

import jakarta.persistence.LockModeType;
import kr.hhplus.be.server.reservation.domain.ScheduleSeat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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
          s.status = kr.hhplus.be.server.reservation.domain.SeatStatus.AVAILABLE
          or (s.status = kr.hhplus.be.server.reservation.domain.SeatStatus.HELD and s.holdExpiresAt < :now)
      )
    order by s.seatNo asc
""")
    List<ScheduleSeat> findAvailableSeats(
            @Param("scheduleId") Long scheduleId,
            @Param("now") LocalDateTime now
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        select s
        from ScheduleSeat s
        where s.schedule.id = :scheduleId
          and s.seatNo = :seatNo
    """)
    Optional<ScheduleSeat> findByScheduleIdAndSeatNoForUpdate(
            @Param("scheduleId") Long scheduleId,
            @Param("seatNo") Integer seatNo
    );

    @Query("""
    select s
    from ScheduleSeat s
    where s.status = kr.hhplus.be.server.reservation.domain.SeatStatus.HELD
      and s.holdExpiresAt < :now
""")
    List<ScheduleSeat> findExpiredHeldSeats(@Param("now") LocalDateTime now);

    @Query("""
    select s from ScheduleSeat s
    where s.schedule.id = :scheduleId and s.seatNo = :seatNo
""")
    Optional<ScheduleSeat> findByScheduleIdAndSeatNo(
            @Param("scheduleId") Long scheduleId,
            @Param("seatNo") int seatNo
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
    update ScheduleSeat s
       set s.status = kr.hhplus.be.server.reservation.domain.SeatStatus.AVAILABLE,
           s.holdUserId = null,
           s.holdExpiresAt = null
     where s.status = kr.hhplus.be.server.reservation.domain.SeatStatus.HELD
       and s.holdExpiresAt < :now
""")
    int releaseExpiredHolds(@Param("now") LocalDateTime now);

}
