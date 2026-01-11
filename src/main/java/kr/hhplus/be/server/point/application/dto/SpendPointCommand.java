package kr.hhplus.be.server.point.application.dto;

import lombok.Value;

@Value
public class SpendPointCommand {
    Long userId;
    Long amount;
}
