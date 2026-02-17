package kr.hhplus.be.server.payment.infrastructure.persistence.jpa;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentOutboxJpaRepository extends JpaRepository<PaymentOutbox, Long> {
}
