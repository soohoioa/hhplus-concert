package kr.hhplus.be.server.point.application.dto;

import lombok.Value;

@Value
public class GetPointBalanceResult {
    Long userId;
    Long balance;
}
