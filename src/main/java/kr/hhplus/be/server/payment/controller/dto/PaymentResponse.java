package kr.hhplus.be.server.payment.controller.dto;

import lombok.Value;

import java.time.LocalDateTime;

@Value
public class PaymentResponse {
    Long paymentId;
    Long userId;
    Long scheduleId;
    Integer seatNo;
    Long amount;
    LocalDateTime paidAt;
}
