package kr.hhplus.be.server.point.port.out;

import kr.hhplus.be.server.point.domain.UserPoint;

import java.util.Optional;

public interface LoadUserPointPort {
    Optional<UserPoint> findByUserId(Long userId);
}
