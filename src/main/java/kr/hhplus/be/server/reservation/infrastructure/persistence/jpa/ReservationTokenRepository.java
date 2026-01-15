package kr.hhplus.be.server.reservation.infrastructure.persistence.jpa;

import kr.hhplus.be.server.reservation.domain.ReservationToken;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import java.time.LocalDateTime;

@Repository
public interface ReservationTokenRepository extends JpaRepository<ReservationToken, Long> {

    Optional<ReservationToken> findByToken(String token);

    @Query("""
        select count(t)
          from ReservationToken t
         where t.status = kr.hhplus.be.server.reservation.domain.TokenStatus.ACTIVE
           and t.expiresAt > :now
    """)
    long countActive(@Param("now") LocalDateTime now);

    @Query("""
        select t
          from ReservationToken t
         where t.status = kr.hhplus.be.server.reservation.domain.TokenStatus.WAITING
           and t.expiresAt > :now
         order by t.id asc
    """)
    List<ReservationToken> findWaitingToPromote(@Param("now") LocalDateTime now, Pageable pageable);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        update ReservationToken t
           set t.status = kr.hhplus.be.server.reservation.domain.TokenStatus.EXPIRED
         where t.status in (
              kr.hhplus.be.server.reservation.domain.TokenStatus.WAITING,
              kr.hhplus.be.server.reservation.domain.TokenStatus.ACTIVE
         )
           and t.expiresAt <= :now
    """)
    int expireTokens(@Param("now") LocalDateTime now);

}
