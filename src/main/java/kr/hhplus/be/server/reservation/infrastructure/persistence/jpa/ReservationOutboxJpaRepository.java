package kr.hhplus.be.server.reservation.infrastructure.persistence.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ReservationOutboxJpaRepository extends JpaRepository<ReservationOutbox, Long> {

    List<ReservationOutbox> findTop100ByStatusOrderByIdAsc(String status);

    @Modifying
    @Query("""
        update ReservationOutbox o
           set o.status = 'SENDING',
               o.updatedAt = CURRENT_TIMESTAMP
         where o.id = :id
           and o.status = 'PENDING'
    """)
    int markSending(@Param("id") Long id);

    @Modifying
    @Query("""
        update ReservationOutbox o
           set o.status = 'SENT',
               o.updatedAt = CURRENT_TIMESTAMP
         where o.id = :id
    """)
    int markSent(@Param("id") Long id);

    @Modifying
    @Query("""
        update ReservationOutbox o
           set o.status = 'FAILED',
               o.retryCount = o.retryCount + 1,
               o.lastError = :error,
               o.updatedAt = CURRENT_TIMESTAMP
         where o.id = :id
    """)
    int markFailed(@Param("id") Long id, @Param("error") String error);
}
