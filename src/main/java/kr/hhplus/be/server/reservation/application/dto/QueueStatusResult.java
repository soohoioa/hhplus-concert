package kr.hhplus.be.server.reservation.application.dto;

import kr.hhplus.be.server.reservation.domain.TokenStatus;
import lombok.Value;

@Value
public class QueueStatusResult {
    String token;
    TokenStatus status;
    Long queueNo; // id를 대기 순번으로 사용
}
