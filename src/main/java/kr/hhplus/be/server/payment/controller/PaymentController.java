package kr.hhplus.be.server.payment.controller;

import kr.hhplus.be.server.payment.application.PayUseCase;
import kr.hhplus.be.server.payment.application.dto.PayCommand;
import kr.hhplus.be.server.payment.application.dto.PayResult;
import kr.hhplus.be.server.payment.dto.PaymentRequest;
import kr.hhplus.be.server.payment.dto.PaymentResponse;
import kr.hhplus.be.server.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/payments")
public class PaymentController {

    private final PayUseCase payUseCase;

    @PostMapping
    public PaymentResponse pay(@RequestBody PaymentRequest paymentRequest) {
        PayResult result = payUseCase.pay(
                new PayCommand(paymentRequest.getUserId(), paymentRequest.getScheduleId(),
                        paymentRequest.getSeatNo(), paymentRequest.getAmount())
        );

        return new PaymentResponse(
                result.getPaymentId(),
                result.getUserId(),
                result.getScheduleId(),
                result.getSeatNo(),
                result.getAmount(),
                result.getPaidAt()
        );
    }

}
