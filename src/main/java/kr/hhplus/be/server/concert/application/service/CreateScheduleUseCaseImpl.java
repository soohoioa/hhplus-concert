package kr.hhplus.be.server.concert.application.service;

import kr.hhplus.be.server.common.error.AppException;
import kr.hhplus.be.server.common.error.ErrorCode;
import kr.hhplus.be.server.concert.application.dto.CreateScheduleCommand;
import kr.hhplus.be.server.concert.application.dto.CreateScheduleResult;
import kr.hhplus.be.server.concert.domain.Concert;
import kr.hhplus.be.server.concert.domain.ConcertSchedule;
import kr.hhplus.be.server.concert.port.out.LoadConcertPort;
import kr.hhplus.be.server.concert.port.out.SaveSchedulePort;
import kr.hhplus.be.server.concert.port.out.SaveSeatsPort;
import kr.hhplus.be.server.reservation.domain.ScheduleSeat;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class CreateScheduleUseCaseImpl implements CreateScheduleUseCase {

    private static final int SEAT_COUNT = 50;

    private final LoadConcertPort loadConcertPort;
    private final SaveSchedulePort saveSchedulePort;
    private final SaveSeatsPort saveSeatsPort;

    @Override
    public CreateScheduleResult create(CreateScheduleCommand command) {
        if (command == null || command.getConcertId() == null || command.getStartAt() == null) {
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }

        Concert concert = loadConcertPort.findById(command.getConcertId())
                .orElseThrow(() -> new AppException(ErrorCode.CONCERT_NOT_FOUND));

        ConcertSchedule schedule = ConcertSchedule.create(concert, command.getStartAt());
        ConcertSchedule saved = saveSchedulePort.save(schedule);

        List<ScheduleSeat> seats = new ArrayList<>(SEAT_COUNT);
        for (int seatNo = 1; seatNo <= SEAT_COUNT; seatNo++) {
            seats.add(ScheduleSeat.create(saved, seatNo));
        }
        saveSeatsPort.saveAll(seats);

        return new CreateScheduleResult(saved.getId(), concert.getId(), saved.getStartAt());
    }

}
