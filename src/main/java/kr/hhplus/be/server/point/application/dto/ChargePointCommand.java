package kr.hhplus.be.server.point.application.dto;

import lombok.Value;

@Value
public class ChargePointCommand {
    Long userId;
    Long amount;
}
