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
    ); // 예약 가능한 좌석 목록 조회

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
    ); // 좌석 1개를 조회하면서 DB 락(비관적 락)을 건다 (PESSIMISTIC_WRITE)

    @Query("""
    select s
    from ScheduleSeat s
    where s.status = kr.hhplus.be.server.reservation.domain.SeatStatus.HELD
      and s.holdExpiresAt < :now
""")
    List<ScheduleSeat> findExpiredHeldSeats(@Param("now") LocalDateTime now); // 만료된 HELD 좌석 목록 조회

    @Query("""
    select s from ScheduleSeat s
    where s.schedule.id = :scheduleId and s.seatNo = :seatNo
""")
    Optional<ScheduleSeat> findByScheduleIdAndSeatNo(
            @Param("scheduleId") Long scheduleId,
            @Param("seatNo") int seatNo
    ); // 좌석 1개 단순 조회

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
    // 만료된 HELD 좌석을 AVAILABLE로 일괄 해제 (UPDATE 한방 처리)

    Optional<ScheduleSeat> findBySchedule_IdAndSeatNo(Long scheduleId, Integer seatNo);
    // scheduleId + seatNo로 좌석 단순 조회 (Spring Data JPA 네이밍 쿼리)

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        update ScheduleSeat s
           set s.status = kr.hhplus.be.server.reservation.domain.SeatStatus.HELD,
               s.holdUserId = :userId,
               s.holdExpiresAt = :expiresAt,
               s.reservedUserId = null,
               s.reservedAt = null
         where s.schedule.id = :scheduleId
           and s.seatNo = :seatNo
           and (
                s.status = kr.hhplus.be.server.reservation.domain.SeatStatus.AVAILABLE
                or (s.status = kr.hhplus.be.server.reservation.domain.SeatStatus.HELD and s.holdExpiresAt < :now)
           )
        """)
    int tryHold(Long scheduleId, Integer seatNo, Long userId,
                LocalDateTime expiresAt, LocalDateTime now);
    // 임시 홀드(HELD) 선점 을 원자적으로 처리하는 핵심 쿼리 (동시성 1명만 성공)

}
