package kr.hhplus.be.server.reservation.infrastructure.scheduler;

import kr.hhplus.be.server.reservation.application.service.ReleaseExpiredHoldsUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class SeatHoldExpireScheduler {

    private final ReleaseExpiredHoldsUseCase releaseExpiredHoldsUseCase;

    /**
     * 30초마다 만료된 HOLD 좌석 정리
     */
    @Scheduled(fixedDelay = 30_000)
    public void releaseExpiredHolds() {
        int released = releaseExpiredHoldsUseCase.releaseExpiredHolds();
        if (released > 0) {
            log.info("Expired seat holds released: {}", released);
        }
    }

}
