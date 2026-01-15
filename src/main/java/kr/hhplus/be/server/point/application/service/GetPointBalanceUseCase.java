package kr.hhplus.be.server.point.application.service;

import kr.hhplus.be.server.point.application.dto.GetPointBalanceQuery;
import kr.hhplus.be.server.point.application.dto.GetPointBalanceResult;

public interface GetPointBalanceUseCase {
    GetPointBalanceResult get(GetPointBalanceQuery query);
}
