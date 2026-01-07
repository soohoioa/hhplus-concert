package kr.hhplus.be.server.payment.application.port;

import kr.hhplus.be.server.point.domain.UserPoint;

public interface SaveUserPointPort {
    UserPoint save(UserPoint userPoint);
}
