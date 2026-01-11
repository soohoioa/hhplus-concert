package kr.hhplus.be.server.point.controller.dto;

import lombok.Value;

@Value
public class PointChargeResponse {
    Long userId;
    Long balance;
}
