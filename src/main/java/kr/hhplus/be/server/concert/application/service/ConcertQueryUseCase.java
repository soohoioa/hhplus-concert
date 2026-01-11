package kr.hhplus.be.server.concert.application.service;

import kr.hhplus.be.server.concert.application.dto.GetAvailableSeatsQuery;
import kr.hhplus.be.server.concert.application.dto.GetAvailableSeatsResult;
import kr.hhplus.be.server.concert.application.dto.GetSchedulesQuery;
import kr.hhplus.be.server.concert.application.dto.ScheduleItem;

import java.util.List;

public interface ConcertQueryUseCase {
    List<ScheduleItem> getSchedules(GetSchedulesQuery query);
    GetAvailableSeatsResult getAvailableSeats(GetAvailableSeatsQuery query);
}
