package kr.hhplus.be.server.point.service;

import kr.hhplus.be.server.common.error.AppException;
import kr.hhplus.be.server.common.error.ErrorCode;
import kr.hhplus.be.server.point.domain.UserPoint;
import kr.hhplus.be.server.point.dto.PointBalanceResponse;
import kr.hhplus.be.server.point.dto.PointChargeRequest;
import kr.hhplus.be.server.point.dto.PointChargeResponse;
import kr.hhplus.be.server.point.repository.UserPointRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PointService {

    private final UserPointRepository userPointRepository;

    @Transactional
    public PointChargeResponse charge(PointChargeRequest pointChargeRequest) {
        validateChargeRequest(pointChargeRequest);

        UserPoint userPoint = userPointRepository.findById(pointChargeRequest.getUserId())
                .orElseGet(() -> userPointRepository.save(UserPoint.init(pointChargeRequest.getUserId())));

        userPoint.charge(pointChargeRequest.getAmount());
        return new PointChargeResponse(pointChargeRequest.getUserId(), userPoint.getBalance());
    }

    @Transactional(readOnly = true)
    public PointBalanceResponse getBalance(Long userId) {
        validateUserId(userId);

        UserPoint userPoint = userPointRepository.findById(userId).orElse(UserPoint.init(userId)); // 없으면 0으로 응답
        return new PointBalanceResponse(userId, userPoint.getBalance());
    }

    private void validateChargeRequest(PointChargeRequest req) {
        if (req == null || req.getUserId() == null) {
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }
        if (req.getAmount() == null || req.getAmount() <= 0) {
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }
    }

    private void validateUserId(Long userId) {
        if (userId == null) {
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }
    }

}
