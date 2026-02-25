package kr.hhplus.be.server.payment.infrastructure.persistence.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PaymentOutboxJpaRepository extends JpaRepository<PaymentOutbox, Long> {
    List<PaymentOutbox> findTop100ByStatusOrderByIdAsc(String status);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        update PaymentOutbox o
        set o.status = 'SENDING', o.updatedAt = CURRENT_TIMESTAMP
        where o.id = :id and o.status = 'PENDING'
    """)
    int markSending(@Param("id") Long id);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        update PaymentOutbox o
        set o.status = 'SENT', o.updatedAt = CURRENT_TIMESTAMP
        where o.id = :id and o.status = 'SENDING'
    """)
    int markSent(@Param("id") Long id);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        update PaymentOutbox o
        set o.status = 'FAILED',
            o.retryCount = o.retryCount + 1,
            o.lastError = :error,
            o.updatedAt = CURRENT_TIMESTAMP
        where o.id = :id and o.status = 'SENDING'
    """)
    int markFailed(@Param("id") Long id, @Param("error") String error);
}
