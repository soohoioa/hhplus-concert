package kr.hhplus.be.server.point.application.service;

import jakarta.persistence.OptimisticLockException;
import kr.hhplus.be.server.common.error.AppException;
import kr.hhplus.be.server.common.error.ErrorCode;
import kr.hhplus.be.server.point.domain.UserPoint;
import kr.hhplus.be.server.point.port.out.LoadUserPointPort;
import kr.hhplus.be.server.point.port.out.SaveUserPointPort;
import lombok.RequiredArgsConstructor;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Service
@RequiredArgsConstructor
public class SpendPointUseCaseImpl implements SpendPointUseCase {

    private static final int MAX_RETRY = 20;

    private final PlatformTransactionManager txManager;

    private final LoadUserPointPort loadUserPointPort;
    private final SaveUserPointPort saveUserPointPort;

    @Override
    public void spend(Long userId, Long amount) {
        TransactionTemplate tx = new TransactionTemplate(txManager);

        int attempt = 0;
        while (true) {
            try {
                tx.execute(status -> {
                    UserPoint point = loadUserPointPort.findByUserId(userId)
                            .orElseThrow(() -> new AppException(ErrorCode.USER_POINT_NOT_FOUND));

                    // 도메인에서 음수/잔액부족 검증
                    point.spend(amount);

                    // save는 선택이지만, Port 구조 유지 위해 호출 (managed 엔티티면 없어도 되지만, 구현체가 save를 전제로 할 수 있음)
                    saveUserPointPort.save(point);

                    // flush로 낙관락 예외를 tx.execute 내부에서 터뜨리게 해서 catch가 100% 잡도록 고정
                    saveUserPointPort.flush();

                    return null;
                });

                return; // 성공 시 종료

            } catch (ObjectOptimisticLockingFailureException | OptimisticLockException e) {
                attempt++;
                if (attempt >= MAX_RETRY) {
                    throw new AppException(ErrorCode.INTERNAL_ERROR);
                }
                // 충돌 완화
                Thread.yield();
            }
        }
    }
}
