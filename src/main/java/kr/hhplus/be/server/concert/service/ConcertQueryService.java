package kr.hhplus.be.server.concert.service;

import kr.hhplus.be.server.common.error.AppException;
import kr.hhplus.be.server.common.error.ErrorCode;
import kr.hhplus.be.server.concert.dto.AvailableSeatsResponse;
import kr.hhplus.be.server.concert.dto.ScheduleResponse;
import kr.hhplus.be.server.concert.repository.ConcertRepository;
import kr.hhplus.be.server.concert.repository.ConcertScheduleRepository;
import kr.hhplus.be.server.reservation.domain.ScheduleSeat;
import kr.hhplus.be.server.reservation.repository.ScheduleSeatRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ConcertQueryService {

    private final ConcertRepository concertRepository;
    private final ConcertScheduleRepository scheduleRepository;
    private final ScheduleSeatRepository seatRepository;

    public List<ScheduleResponse> getSchedules(Long concertId) {
        concertRepository.findById(concertId).orElseThrow(() -> new AppException(ErrorCode.CONCERT_NOT_FOUND));

        return scheduleRepository.findByConcertIdOrderByStartAtAsc(concertId).stream()
                .map(s -> new ScheduleResponse(s.getId(), s.getStartAt()))
                .toList();
    }

    public AvailableSeatsResponse getAvailableSeats(Long concertId, Long scheduleId) {
        // 1) 콘서트 존재 여부 먼저 확인
        concertRepository.findById(concertId)
                .orElseThrow(() -> new AppException(ErrorCode.CONCERT_NOT_FOUND));

        // 2) 해당 콘서트의 스케줄인지 확인
        scheduleRepository.findByIdAndConcertId(scheduleId, concertId)
                .orElseThrow(() -> new AppException(ErrorCode.SCHEDULE_NOT_FOUND));

        LocalDateTime now = LocalDateTime.now();
        List<Integer> seatNos = seatRepository.findAvailableSeats(scheduleId, now).stream()
                .map(ScheduleSeat::getSeatNo)
                .toList();

        return new AvailableSeatsResponse(scheduleId, seatNos);
    }

}
