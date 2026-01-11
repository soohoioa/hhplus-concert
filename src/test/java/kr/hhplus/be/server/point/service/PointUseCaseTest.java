package kr.hhplus.be.server.point.service;

import kr.hhplus.be.server.common.error.AppException;
import kr.hhplus.be.server.common.error.ErrorCode;
import kr.hhplus.be.server.point.application.dto.ChargePointCommand;
import kr.hhplus.be.server.point.application.dto.ChargePointResult;
import kr.hhplus.be.server.point.application.dto.GetPointBalanceQuery;
import kr.hhplus.be.server.point.application.dto.GetPointBalanceResult;
import kr.hhplus.be.server.point.application.service.ChargePointUseCase;
import kr.hhplus.be.server.point.application.service.GetPointBalanceUseCase;
import kr.hhplus.be.server.point.domain.UserPoint;
import kr.hhplus.be.server.point.infrastructure.persistence.jpa.UserPointJpaRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class PointUseCaseTest { // 포인트 충전/조회 + 포인트 입력 검증 실패

    @Autowired
    ChargePointUseCase chargePointUseCase;

    @Autowired
    GetPointBalanceUseCase getPointBalanceUseCase;

    @Autowired
    UserPointJpaRepository userPointJpaRepository;

    @Test
    void 포인트를_충전한다() {
        // when
        ChargePointResult result = chargePointUseCase.charge(new ChargePointCommand(1L, 1000L));

        // then
        assertThat(result.getBalance()).isEqualTo(1000L);
    }

    @Test
    void 포인트를_조회한다() {
        // given
        userPointJpaRepository.save(UserPoint.init(1L));

        // when
        GetPointBalanceResult result = getPointBalanceUseCase.get(new GetPointBalanceQuery(1L));

        // then
        assertThat(result.getBalance()).isEqualTo(0L);
    }

    @ParameterizedTest(name = "충전금액이 0이면 INVALID_REQUEST 발생")
    @ValueSource(longs = {0L, -100L})
    void 충전금액이_유효하지_않으면_에러발생(long amount) {
        // when
        AppException ex = assertThrows(AppException.class,
                () -> chargePointUseCase.charge(new ChargePointCommand(1L, amount))
        );

        // then
        assertEquals(ErrorCode.INVALID_REQUEST, ex.getErrorCode());
    }

}