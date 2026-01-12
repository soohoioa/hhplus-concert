package kr.hhplus.be.server.reservation.application.service;

import kr.hhplus.be.server.common.error.AppException;
import kr.hhplus.be.server.common.error.ErrorCode;
import kr.hhplus.be.server.reservation.application.dto.QueueStatusResult;
import kr.hhplus.be.server.reservation.domain.ReservationToken;
import kr.hhplus.be.server.reservation.domain.TokenStatus;
import kr.hhplus.be.server.reservation.infrastructure.persistence.jpa.ReservationTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReservationTokenUseCaseImpl implements ReservationTokenUseCase {

    private final ReservationTokenRepository tokenRepository;

    // 토큰 TTL / 동시에 ACTIVE 유지 인원
    private static final Duration TOKEN_TTL = Duration.ofMinutes(30);
    private static final int ACTIVE_WINDOW = 100;

    @Override
    @Transactional
    public ReservationToken issue(Long userId) {
        LocalDateTime now = LocalDateTime.now();
        ReservationToken t = ReservationToken.issue(userId, now, now.plus(TOKEN_TTL));
        tokenRepository.save(t);

        // 발급 시점에 ACTIVE 승격 진행(간단/안정)
        refreshActive();
        return t;
    }

    @Override
    @Transactional(readOnly = true)
    public QueueStatusResult status(String token) {
        ReservationToken t = tokenRepository.findByToken(token)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND));

        return new QueueStatusResult(t.getToken(), t.getStatus(), t.getId());
    }

    @Override
    @Transactional
    public ReservationToken validateActive(String token) {
        LocalDateTime now = LocalDateTime.now();

        // 만료 처리(정리)
        tokenRepository.expireTokens(now);

        ReservationToken t = tokenRepository.findByToken(token)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND));

        if (t.isExpired(now) || t.getStatus() == TokenStatus.EXPIRED) {
            throw new AppException(ErrorCode.INVALID_REQUEST); // 필요하면 TOKEN_EXPIRED로 분리
        }
        if (t.getStatus() != TokenStatus.ACTIVE) {
            throw new AppException(ErrorCode.INVALID_REQUEST); // 필요하면 QUEUE_WAITING으로 분리
        }
        return t;
    }

    @Override
    @Transactional
    public void consume(String token) {
        ReservationToken t = tokenRepository.findByToken(token)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND));

        t.consume(LocalDateTime.now());

        // 한 명 소비되면 다음 대기자 ACTIVE 승격
        refreshActive();
    }

    @Override
    @Transactional
    public void refreshActive() {
        LocalDateTime now = LocalDateTime.now();

        // 먼저 만료 정리
        tokenRepository.expireTokens(now);

        long activeCount = tokenRepository.countActive(now);
        int need = (int) Math.max(0, ACTIVE_WINDOW - activeCount);
        if (need <= 0) return;

        List<ReservationToken> promote = tokenRepository.findWaitingToPromote(now, PageRequest.of(0, need));
        for (ReservationToken t : promote) {
            t.activate(now);
        }
    }
}
