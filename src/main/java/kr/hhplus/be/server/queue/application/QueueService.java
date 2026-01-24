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

        if (rank == null) {
            if (ready) {
                // permit 유효: 통과 상태
                return new StatusResult(0, 0, true);
            }
            //  permit 만료(or 아직 permit 못받음)인데 큐에 없다면 "만료"로 터뜨리지 말고, 그냥 ready=false 상태로 응답 (폴링 가능)
            return new StatusResult(-1, -1, false);
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
