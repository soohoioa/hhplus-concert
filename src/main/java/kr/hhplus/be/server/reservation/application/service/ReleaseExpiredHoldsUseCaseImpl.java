package kr.hhplus.be.server.reservation.application.service;

import kr.hhplus.be.server.reservation.port.out.ReleaseExpiredHoldsPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional
public class ReleaseExpiredHoldsUseCaseImpl implements ReleaseExpiredHoldsUseCase {

    private final ReleaseExpiredHoldsPort releaseExpiredHoldsPort;

    @Override
    public int releaseExpired() {
        return releaseExpiredHoldsPort.releaseExpiredHolds(LocalDateTime.now());
    }
}
