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

        String permitTtlKey = QueueKeys.schedulePermitTtlKey(scheduleId, claims.userUuid());
        boolean ready = queueRedisRepository.hasValidPermit(permitTtlKey);

        // permit 유효하면 즉시 통과
        if (ready) {
            return new StatusResult(0, 0, true);
        }

        // permit이 없으면 큐에 남아있는지 live rank 확인
        Long liveRank = queueRedisRepository.getRank(claims.queueKey(), claims.userUuid());
        if (liveRank != null) {
            return new StatusResult(liveRank, estimateEtaSeconds(liveRank), false);
        }

        // 큐에서는 빠졌는데 permit이 아직 없거나(타이밍), permit 만료된 경우 → snapRank로 UX 안정화
        long snapRank = Math.max(0L, claims.snapRank());
        return new StatusResult(snapRank, estimateEtaSeconds(snapRank), false);
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
