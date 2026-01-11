package kr.hhplus.be.server.point.controller.dto;

import lombok.Value;

@Value
public class PointBalanceResponse {
    Long userId;
    Long balance;
}
