package kr.hhplus.be.server.point;

import kr.hhplus.be.server.common.error.AppException;
import kr.hhplus.be.server.common.error.ErrorCode;
import kr.hhplus.be.server.point.application.service.SpendPointUseCase;
import kr.hhplus.be.server.point.domain.UserPoint;
import kr.hhplus.be.server.point.infrastructure.persistence.jpa.UserPointJpaRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
public class PointSpendConcurrencyIT {

    @Autowired
    SpendPointUseCase spendPointUseCase;
    @Autowired
    UserPointJpaRepository userPointJpaRepository;

    @Autowired
    PlatformTransactionManager txManager;

    @Test
    void 동시에_spend해도_음수잔액이_발생하지_않고_성공실패_건수가_기대값과_일치한다() throws Exception {
        TransactionTemplate tx = new TransactionTemplate(txManager);

        Long userId = 1L;
        long initial = 50_000L;
        long spend = 10_000L;

        // given: 커밋 보장 트랜잭션에서 포인트 초기화
        tx.execute(status -> {
            if (userPointJpaRepository.existsById(userId)) {
                userPointJpaRepository.deleteById(userId);
            }
            UserPoint p = UserPoint.init(userId);
            p.charge(initial);
            userPointJpaRepository.save(p);
            return null;
        });

        int threadCount = 10;
        ExecutorService pool = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startGate = new CountDownLatch(1);
        CountDownLatch doneGate = new CountDownLatch(threadCount);

        AtomicInteger success = new AtomicInteger(0);
        AtomicInteger failInsufficient = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            pool.submit(() -> {
                try {
                    startGate.await();
                    try {
                        spendPointUseCase.spend(userId, spend);
                        success.incrementAndGet();
                    } catch (AppException e) {
                        if (e.getErrorCode() == ErrorCode.INSUFFICIENT_BALANCE) {
                            failInsufficient.incrementAndGet();
                        } else {
                            // 예상치 못한 예외는 실패로 카운트해도 되고, 로그로 남겨도 됨
                        }
                    }
                } catch (InterruptedException ignored) {
                } finally {
                    doneGate.countDown();
                }
            });
        }

        startGate.countDown();
        doneGate.await();

        pool.shutdown();
        pool.awaitTermination(5, TimeUnit.SECONDS);

        // then
        assertThat(success.get()).isEqualTo(5);
        assertThat(failInsufficient.get()).isEqualTo(5);

        UserPoint latest = userPointJpaRepository.findById(userId).orElseThrow();
        assertThat(latest.getBalance()).isEqualTo(0L);
        assertThat(latest.getBalance()).isGreaterThanOrEqualTo(0L);
    }
}
