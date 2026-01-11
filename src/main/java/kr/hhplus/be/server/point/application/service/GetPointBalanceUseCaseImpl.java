package kr.hhplus.be.server.point.application.service;

import kr.hhplus.be.server.common.error.AppException;
import kr.hhplus.be.server.common.error.ErrorCode;
import kr.hhplus.be.server.point.application.dto.GetPointBalanceQuery;
import kr.hhplus.be.server.point.application.dto.GetPointBalanceResult;
import kr.hhplus.be.server.point.domain.UserPoint;
import kr.hhplus.be.server.point.port.out.LoadUserPointPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GetPointBalanceUseCaseImpl implements GetPointBalanceUseCase {

    private final LoadUserPointPort loadUserPointPort;

    @Override
    public GetPointBalanceResult get(GetPointBalanceQuery query) {
        validate(query);

        UserPoint userPoint = loadUserPointPort.findByUserId(query.getUserId())
                .orElse(UserPoint.init(query.getUserId())); // 없으면 0 응답(저장 X)

        return new GetPointBalanceResult(query.getUserId(), userPoint.getBalance());
    }

    private void validate(GetPointBalanceQuery query) {
        if (query == null || query.getUserId() == null) {
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }
    }
}
