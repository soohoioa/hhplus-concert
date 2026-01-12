package kr.hhplus.be.server.queue.application;

import kr.hhplus.be.server.common.error.AppException;
import kr.hhplus.be.server.common.error.ErrorCode;
import kr.hhplus.be.server.queue.infrastructure.RedisQueueRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class QueueService {

    private final RedisQueueRepository redisQueueRepository;
    private final QueueTokenService queueTokenService;

    // 대기시간 추정은 일단 단순 상수로 시작(고도화에서 개선)
    private static final long AVG_PROCESS_SECONDS = 3;

    /**
     * 토큰 발급: (userUuid, queueKey) 기준으로 대기열 등록 + JWT 발급
     */
    public IssueResult issueToken(String userUuid, String queueKey) {
        long rank = redisQueueRepository.registerIfAbsent(queueKey, userUuid);
        if (rank < 0) throw new AppException(ErrorCode.QUEUE_NOT_FOUND);

        String token = queueTokenService.issue(userUuid, queueKey, rank);
        return new IssueResult(token, rank, estimateEtaSeconds(rank));
    }

    /**
     * 폴링 조회: 토큰 -> Redis에서 현재 rank 다시 계산
     */
    public StatusResult getStatus(String token) {
        QueueTokenService.QueueTokenClaims claims = parseOrThrow(token);

        Long rank = redisQueueRepository.getRank(claims.queueKey(), claims.userUuid());
        if (rank == null) throw new AppException(ErrorCode.QUEUE_EXPIRED);

        return new StatusResult(rank, estimateEtaSeconds(rank), rank == 0);
    }

    /**
     * 모든 API 진입 전 검증: "내 차례인가?"
     */
    public void validateReady(String token) {
        StatusResult status = getStatus(token);
        if (!status.ready()) throw new AppException(ErrorCode.QUEUE_NOT_READY);
    }

    private QueueTokenService.QueueTokenClaims parseOrThrow(String token) {
        try {
            return queueTokenService.parse(token);
        } catch (Exception e) {
            throw new AppException(ErrorCode.QUEUE_TOKEN_INVALID);
        }
    }

    private long estimateEtaSeconds(long rank) {
        return rank * AVG_PROCESS_SECONDS;
    }

    public record IssueResult(String queueToken, long rank, long etaSeconds) {}
    public record StatusResult(long rank, long etaSeconds, boolean ready) {}

}
