package kr.hhplus.be.server.concert.application.service;

import kr.hhplus.be.server.concert.application.dto.CreateScheduleCommand;
import kr.hhplus.be.server.concert.application.dto.CreateScheduleResult;

public interface CreateScheduleUseCase {
    CreateScheduleResult create(CreateScheduleCommand command);
}
