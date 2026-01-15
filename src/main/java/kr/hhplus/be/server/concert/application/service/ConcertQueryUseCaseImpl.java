package kr.hhplus.be.server.concert.application.service;

import kr.hhplus.be.server.common.error.AppException;
import kr.hhplus.be.server.common.error.ErrorCode;
import kr.hhplus.be.server.concert.application.dto.GetAvailableSeatsQuery;
import kr.hhplus.be.server.concert.application.dto.GetAvailableSeatsResult;
import kr.hhplus.be.server.concert.application.dto.GetSchedulesQuery;
import kr.hhplus.be.server.concert.application.dto.ScheduleItem;
import kr.hhplus.be.server.concert.port.out.FindAvailableSeatsPort;
import kr.hhplus.be.server.concert.port.out.FindSchedulesPort;
import kr.hhplus.be.server.concert.port.out.LoadConcertPort;
import kr.hhplus.be.server.concert.port.out.LoadSchedulePort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ConcertQueryUseCaseImpl implements ConcertQueryUseCase {

    private final LoadConcertPort loadConcertPort;
    private final LoadSchedulePort loadSchedulePort;
    private final FindSchedulesPort findSchedulesPort;
    private final FindAvailableSeatsPort findAvailableSeatsPort;

    @Override
    public List<ScheduleItem> getSchedules(GetSchedulesQuery query) {
        if (query == null || query.getConcertId() == null) {
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }

        loadConcertPort.findById(query.getConcertId())
                .orElseThrow(() -> new AppException(ErrorCode.CONCERT_NOT_FOUND));

        return findSchedulesPort.findByConcertIdOrderByStartAtAsc(query.getConcertId()).stream()
                .map(s -> new ScheduleItem(s.getId(), s.getStartAt()))
                .toList();
    }

    @Override
    public GetAvailableSeatsResult getAvailableSeats(GetAvailableSeatsQuery query) {
        if (query == null || query.getConcertId() == null || query.getScheduleId() == null) {
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }

        loadConcertPort.findById(query.getConcertId())
                .orElseThrow(() -> new AppException(ErrorCode.CONCERT_NOT_FOUND));

        loadSchedulePort.findByIdAndConcertId(query.getScheduleId(), query.getConcertId())
                .orElseThrow(() -> new AppException(ErrorCode.SCHEDULE_NOT_FOUND));

        LocalDateTime now = LocalDateTime.now();
        List<Integer> seatNos = findAvailableSeatsPort.findAvailableSeatNos(query.getScheduleId(), now);

        return new GetAvailableSeatsResult(query.getScheduleId(), seatNos);
    }
}
