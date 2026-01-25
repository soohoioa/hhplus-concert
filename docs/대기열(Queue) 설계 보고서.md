# 대기열(Queue) 설계 및 테스트 보고서

## 1. 개요
콘서트 예약 시나리오에서 대규모 동시 접근을 제어하기 위해 Redis 기반 대기열 시스템을 설계하고, 이를 통합 테스트로 검증한 내용이다.

인기 콘서트의 경우 특정 시점에 트래픽이 집중되며, 예약 API를 그대로 노출할 경우 서버 부하 및 데이터 정합성 문제가 발생할 수 있다.
이를 해결하기 위해 예약 API 진입 전 대기열을 구성하였다.

---

## 2. 설계 목표
- 예약 API 진입 전 트래픽 제어
- 동시에 처리 가능한 사용자 수 제한
- 순번(rank) 기반이 아닌 Permit(입장권) 기반 접근 제어
- Permit 만료 시 다음 사용자에게 자동 기회 이전

---

## 3. Redis 자료구조 설계

| 목적 | Redis Key | 자료구조 |
|----|----|----|
| 대기열 | queue:schedule:{scheduleId} | ZSET |
| Permit 집합 | queue:permit:schedule:{scheduleId} | SET |
| Permit TTL | queue:permit:ttl:schedule:{scheduleId}:{userUuid} | STRING |
| 활성 스케줄 | queue:schedules:active | SET |

### 설계 포인트
- ZSET score는 대기열 진입 시각(ms)
- Permit은 TTL 기반으로 자동 만료
- Permit 만료 시 별도 정리 없이 다음 사용자에게 기회 이전

---

## 4. 대기열 처리 흐름

1. 사용자가 대기열 토큰 발급 요청
2. Redis ZSET에 대기열 등록
3. 스케줄러가 주기적으로 상위 N명에게 Permit 발급
4. Permit을 받은 사용자만 예약 API 접근 가능
5. Permit TTL 만료 시 다음 사용자에게 자동 이전

---

## 5. Permit 기반 접근 제어 설계

기존의 단순 순번 기반 방식은 차례가 되었는지를 정확히 제어하기 어렵다는 한계가 있다.

Permit을 통해 Permit을 가진 사용자만 ready 상태, Permit TTL로 장시간 점유 방지, 동시에 접근 가능한 사용자 수를 정확히 제한을 정리한다.

---

## 6. 통합 테스트 (QueuePermitSchedulerIT)

### 테스트 목적
- 동시에 Permit이 N명만 발급되는지 검증
- Permit TTL 만료 시 다음 사용자에게 기회가 이전되는지 검증

### 테스트 시나리오
1. 대기자 3명 등록
2. permit.batch-size = 1
3. 스케줄러 실행 → 1명만 ready
4. Permit TTL 만료
5. 스케줄러 재실행 → 다음 사용자 ready

### 검증 결과
- 동시에 ready 상태인 사용자는 항상 1명
- Permit 만료 후 순차적으로 다음 사용자에게 기회 이전

---

## 7. 결론
Redis 기반 Permit 대기열을 통해
예약 API 보호, 서버 부하 완화, 공정한 접근 제어를 달성하였다.
TTL 기반 설계로 단순하면서도 확장 가능한 구조를 유지한다.
