package kr.hhplus.be.server.reservation.port.out;

import java.time.LocalDateTime;

public interface ReleaseExpiredHoldsPort {
    int releaseExpiredHolds(LocalDateTime now);
}
