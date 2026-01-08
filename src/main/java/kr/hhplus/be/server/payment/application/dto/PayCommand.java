package kr.hhplus.be.server.payment.application.dto;

import lombok.Value;

@Value
public class PayCommand {
    Long userId;
    Long scheduleId;
    Integer seatNo;
    Long amount;
}
