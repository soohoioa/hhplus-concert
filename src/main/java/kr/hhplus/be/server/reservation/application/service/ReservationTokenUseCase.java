package kr.hhplus.be.server.reservation.application.service;

import kr.hhplus.be.server.reservation.application.dto.QueueStatusResult;
import kr.hhplus.be.server.reservation.domain.ReservationToken;

public interface ReservationTokenUseCase {
    ReservationToken issue(Long userId);

    QueueStatusResult status(String token);

    ReservationToken validateActive(String token);

    void consume(String token);

    void refreshActive();
}
