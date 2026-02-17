package kr.hhplus.be.server.payment.application.event;

import lombok.Value;

import java.time.LocalDateTime;

@Value
public class PaymentCompletedEvent {
    Long paymentId;
    Long userId;
    Long scheduleId;
    Integer seatNo;
    Long amount;
    LocalDateTime paidAt;
}
