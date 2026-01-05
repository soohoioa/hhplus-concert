package kr.hhplus.be.server.payment.dto;

import lombok.Value;

@Value
public class PaymentRequest  {
    Long userId;
    Long scheduleId;
    Integer seatNo;
    Long amount;
}
