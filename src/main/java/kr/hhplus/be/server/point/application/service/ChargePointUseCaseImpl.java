package kr.hhplus.be.server.point.application.service;


import kr.hhplus.be.server.common.error.AppException;
import kr.hhplus.be.server.common.error.ErrorCode;
import kr.hhplus.be.server.point.application.dto.ChargePointCommand;
import kr.hhplus.be.server.point.application.dto.ChargePointResult;
import kr.hhplus.be.server.point.domain.UserPoint;
import kr.hhplus.be.server.point.port.out.LoadUserPointPort;
import kr.hhplus.be.server.point.port.out.SaveUserPointPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class ChargePointUseCaseImpl implements ChargePointUseCase {

    private final LoadUserPointPort loadUserPointPort;
    private final SaveUserPointPort saveUserPointPort;

    @Override
    public ChargePointResult charge(ChargePointCommand command) {
        validateCharge(command);

        UserPoint userPoint = loadUserPointPort.findByUserId(command.getUserId())
                .orElseGet(() -> saveUserPointPort.save(UserPoint.init(command.getUserId())));

        userPoint.charge(command.getAmount());
        // JPA라면 더티체킹으로 저장되지만, Port 관점에서 저장 의도를 명확히
        saveUserPointPort.save(userPoint);

        return new ChargePointResult(command.getUserId(), userPoint.getBalance());
    }

    private void validateCharge(ChargePointCommand command) {
        if (command == null || command.getUserId() == null) {
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }
        if (command.getAmount() == null || command.getAmount() <= 0) {
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }
    }
}
