package kr.hhplus.be.server.payment.infrastructure.persistence.jpa;

import kr.hhplus.be.server.payment.domain.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PaymentJpaRepository extends JpaRepository<Payment, Long> {
}
