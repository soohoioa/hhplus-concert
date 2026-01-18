package kr.hhplus.be.server.payment.application.service;

import kr.hhplus.be.server.common.error.AppException;
import kr.hhplus.be.server.common.error.ErrorCode;
import kr.hhplus.be.server.common.lock.DistributedLockExecutor;
import kr.hhplus.be.server.payment.application.dto.PayCommand;
import kr.hhplus.be.server.payment.application.dto.PayResult;
import kr.hhplus.be.server.payment.port.out.CreatePaymentPort;
import kr.hhplus.be.server.payment.domain.Payment;
import kr.hhplus.be.server.point.application.service.SpendPointUseCase;
import kr.hhplus.be.server.point.domain.UserPoint;
import kr.hhplus.be.server.reservation.port.out.LoadSeatForUpdatePort;
import kr.hhplus.be.server.reservation.domain.ScheduleSeat;
import kr.hhplus.be.server.reservation.domain.SeatStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Transactional
public class PayUseCaseImpl implements PayUseCase {

    private final DistributedLockExecutor lockExecutor;
    private final PayService payService;

    @Override
    public PayResult pay(PayCommand command) {

        // 좌석(임계자원) 단위로 Hold와 동일한 락 키 사용
        String lockKey = "lock:seat:%d:%d"
                .formatted(command.getScheduleId(), command.getSeatNo());

        return lockExecutor.executeWithLock(
                lockKey,
                Duration.ofSeconds(5),
                () -> payService.pay(command)
        );
    }

}
