package kr.hhplus.be.server.concert.port.out;

import java.time.LocalDateTime;
import java.util.List;

public interface FindAvailableSeatsPort {
    List<Integer> findAvailableSeatNos(Long scheduleId, LocalDateTime now);
}
