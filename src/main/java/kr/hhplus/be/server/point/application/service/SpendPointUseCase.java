package kr.hhplus.be.server.point.application.service;

public interface SpendPointUseCase {
    void spend(Long userId, Long amount);
}
