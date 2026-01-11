package kr.hhplus.be.server.concert.port.out;

import kr.hhplus.be.server.reservation.domain.ScheduleSeat;

import java.util.List;

public interface SaveSeatsPort {
    void saveAll(List<ScheduleSeat> seats);
}
