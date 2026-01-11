package kr.hhplus.be.server.point.application.service;

import kr.hhplus.be.server.common.error.AppException;
import kr.hhplus.be.server.common.error.ErrorCode;
import kr.hhplus.be.server.point.domain.UserPoint;
import kr.hhplus.be.server.point.port.out.LoadUserPointPort;
import kr.hhplus.be.server.point.port.out.SaveUserPointPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SpendPointUseCaseImpl implements SpendPointUseCase {

    private final LoadUserPointPort loadUserPointPort;
    private final SaveUserPointPort saveUserPointPort;

    @Override
    public void spend(Long userId, Long amount) {
        UserPoint point = loadUserPointPort.findByUserId(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_POINT_NOT_FOUND));

        point.spend(amount);
        saveUserPointPort.save(point);
    }
}
