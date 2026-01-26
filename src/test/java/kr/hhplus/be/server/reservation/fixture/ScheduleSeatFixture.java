package kr.hhplus.be.server.reservation.fixture;

import kr.hhplus.be.server.concert.domain.ConcertSchedule;
import kr.hhplus.be.server.reservation.domain.ScheduleSeat;

import java.time.LocalDateTime;

public class ScheduleSeatFixture {
    public static ScheduleSeat held(
            ConcertSchedule schedule,
            int seatNo,
            Long userId,
            LocalDateTime expiresAt
    ) {
        ScheduleSeat seat = ScheduleSeat.create(schedule, seatNo);
        seat.hold(userId, expiresAt);
        return seat;
    }

    public static ScheduleSeat available(
            ConcertSchedule schedule,
            int seatNo
    ) {
        return ScheduleSeat.create(schedule, seatNo);
    }
}
