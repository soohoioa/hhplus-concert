package kr.hhplus.be.server.concert.port.out;

import kr.hhplus.be.server.concert.domain.ConcertSchedule;

public interface SaveSchedulePort {
    ConcertSchedule save(ConcertSchedule schedule);
}
