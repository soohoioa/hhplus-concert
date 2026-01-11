package kr.hhplus.be.server.payment.infrastructure.persistence.adapter;

import kr.hhplus.be.server.payment.port.out.CreatePaymentPort;
import kr.hhplus.be.server.payment.domain.Payment;
import kr.hhplus.be.server.payment.infrastructure.persistence.jpa.PaymentJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class PaymentJpaAdapter implements CreatePaymentPort {

    private final PaymentJpaRepository paymentJpaRepository;

    @Override
    public Payment save(Payment payment) {
        return paymentJpaRepository.save(payment);
    }
}
