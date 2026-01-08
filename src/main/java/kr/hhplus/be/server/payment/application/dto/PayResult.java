package kr.hhplus.be.server.payment.application.dto;

import lombok.Value;

import java.time.LocalDateTime;

@Value
public class PayResult {
    Long paymentId;
    Long userId;
    Long scheduleId;
    Integer seatNo;
    Long amount;
    LocalDateTime paidAt;
}
