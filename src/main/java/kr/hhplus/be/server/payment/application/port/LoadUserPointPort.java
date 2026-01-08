package kr.hhplus.be.server.payment.application.port;

import kr.hhplus.be.server.point.domain.UserPoint;

import java.util.Optional;

public interface LoadUserPointPort {
    Optional<UserPoint> findByUserId(Long userId);
}
