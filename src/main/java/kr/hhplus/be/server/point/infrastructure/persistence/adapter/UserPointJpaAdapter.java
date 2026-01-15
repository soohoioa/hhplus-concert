package kr.hhplus.be.server.point.infrastructure.persistence.adapter;

import kr.hhplus.be.server.point.domain.UserPoint;
import kr.hhplus.be.server.point.infrastructure.persistence.jpa.UserPointJpaRepository;
import kr.hhplus.be.server.point.port.out.LoadUserPointPort;
import kr.hhplus.be.server.point.port.out.SaveUserPointPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class UserPointJpaAdapter implements LoadUserPointPort, SaveUserPointPort {

    private final UserPointJpaRepository userPointJpaRepository;

    @Override
    public Optional<UserPoint> findByUserId(Long userId) {
        return userPointJpaRepository.findById(userId);
    }

    @Override
    public UserPoint save(UserPoint userPoint) {
        return userPointJpaRepository.save(userPoint);
    }

}
