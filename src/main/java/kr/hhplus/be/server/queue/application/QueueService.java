package kr.hhplus.be.server.queue.application;

import kr.hhplus.be.server.common.error.AppException;
import kr.hhplus.be.server.common.error.ErrorCode;
import kr.hhplus.be.server.queue.infrastructure.QueueRedisRepository;
import kr.hhplus.be.server.queue.support.QueueKeys;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class QueueService {

    private final QueueRedisRepository queueRedisRepository;
    private final QueueTokenService queueTokenService;

    // 대기시간 추정은 일단 단순 상수로 시작(고도화에서 개선)
    private static final long AVG_PROCESS_SECONDS = 3;

    /**
     * scheduleId 기반
     */
    public IssueResult issueToken(String userUuid, Long scheduleId) {
        String queueKey = QueueKeys.scheduleQueueKey(scheduleId);

        // 활성 스케줄 등록
        queueRedisRepository.addActiveSchedule(QueueKeys.activeSchedulesKey(), scheduleId);

        long rank = queueRedisRepository.registerIfAbsent(queueKey, userUuid);
        if (rank < 0) throw new AppException(ErrorCode.QUEUE_NOT_FOUND);

        String token = queueTokenService.issue(userUuid, queueKey, rank);
        return new IssueResult(token, rank, estimateEtaSeconds(rank));
    }

    /**
     *  queueKey 기반 (테스트/레거시 호환용)
     * - queueKey가 "queue:schedule:{id}" 형태면 scheduleId를 추출해 active schedule 등록까지 수행
     * - 그 외의 임의 key는 "테스트용 큐"로 간주하고 active 등록 없이 그대로 동작
     */
    public IssueResult issueToken(String userUuid, String queueKey) {
        Long scheduleId = extractScheduleIdFromQueueKey(queueKey);
        if (scheduleId != null) {
            queueRedisRepository.addActiveSchedule(QueueKeys.activeSchedulesKey(), scheduleId);
        }

        long rank = queueRedisRepository.registerIfAbsent(queueKey, userUuid);
        if (rank < 0) throw new AppException(ErrorCode.QUEUE_NOT_FOUND);

        String token = queueTokenService.issue(userUuid, queueKey, rank);
        return new IssueResult(token, rank, estimateEtaSeconds(rank));
    }

    public StatusResult getStatus(String token) {
        QueueTokenService.QueueTokenClaims claims = parseOrThrow(token);

        Long scheduleId = extractScheduleIdFromQueueKey(claims.queueKey());
        if (scheduleId == null) throw new AppException(ErrorCode.QUEUE_TOKEN_INVALID);

        String permitKey = QueueKeys.schedulePermitKey(scheduleId);
        String permitTtlKey = QueueKeys.schedulePermitTtlKey(scheduleId, claims.userUuid());

        boolean ready = queueRedisRepository.hasValidPermit(permitKey, permitTtlKey, claims.userUuid());

        Long rank = queueRedisRepository.getRank(claims.queueKey(), claims.userUuid());

        // 큐에서 제거됐더라도 permit이 유효하면 통과 상태로 본다
        if (rank == null) {
            if (ready) {
                return new StatusResult(0, 0, true);
            }
            throw new AppException(ErrorCode.QUEUE_EXPIRED);
        }

        return new StatusResult(rank, estimateEtaSeconds(rank), ready);
    }

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

    /**
     * queueKey = "queue:schedule:{scheduleId}" 형태를 가정
     */
    private Long extractScheduleIdFromQueueKey(String queueKey) {
        // "queue:schedule:" prefix 이후 숫자
        String prefix = "queue:schedule:";
        if (queueKey == null || !queueKey.startsWith(prefix)) return null;
        String idStr = queueKey.substring(prefix.length());
        try {
            return Long.parseLong(idStr);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public record IssueResult(String queueToken, long rank, long etaSeconds) {}
    public record StatusResult(long rank, long etaSeconds, boolean ready) {}

}
