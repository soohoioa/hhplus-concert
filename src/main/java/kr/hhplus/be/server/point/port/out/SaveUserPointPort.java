package kr.hhplus.be.server.point.port.out;

import kr.hhplus.be.server.point.domain.UserPoint;

public interface SaveUserPointPort {
    UserPoint save(UserPoint userPoint);
    void flush();
}
