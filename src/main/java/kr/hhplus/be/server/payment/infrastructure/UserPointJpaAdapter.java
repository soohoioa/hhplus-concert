package kr.hhplus.be.server.payment.infrastructure;

import kr.hhplus.be.server.payment.application.port.LoadUserPointPort;
import kr.hhplus.be.server.payment.application.port.SaveUserPointPort;
import kr.hhplus.be.server.point.domain.UserPoint;
import kr.hhplus.be.server.point.repository.UserPointRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class UserPointJpaAdapter implements LoadUserPointPort, SaveUserPointPort {

    private final UserPointRepository userPointRepository;

    @Override
    public Optional<UserPoint> findByUserId(Long userId) {
        return userPointRepository.findById(userId);
    }

    @Override
    public UserPoint save(UserPoint userPoint) {
        return userPointRepository.save(userPoint);
    }

}
