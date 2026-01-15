package kr.hhplus.be.server.point.controller.dto;

import lombok.Value;

@Value
public class PointChargeRequest {
    Long userId;
    Long amount;
}
