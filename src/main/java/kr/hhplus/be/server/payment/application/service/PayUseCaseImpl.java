package kr.hhplus.be.server.payment.application.service;

import kr.hhplus.be.server.common.lock.DistributedLockExecutor;
import kr.hhplus.be.server.payment.application.dto.PayCommand;
import kr.hhplus.be.server.payment.application.dto.PayResult;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;

@Service
@RequiredArgsConstructor
@Transactional
public class PayUseCaseImpl implements PayUseCase {

    private final DistributedLockExecutor lockExecutor;
    private final PayService payService;

    @Override
    @CacheEvict(cacheNames = "concert:available-seats", key = "#command.scheduleId")
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
