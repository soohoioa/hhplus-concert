# Redis 기반 분산락 적용 보고서 (좌석 HOLD / 결제 PAY)

## 1. 목표
동시 요청 환경에서 다음과 같은 동시성 문제를 방지한다.

- 동일 좌석에 대한 동시 예약/결제 요청 → **중복 예약/중복 결제**
- 좌석 임시 배정(HOLD) 동시 요청 → **여러 사용자가 동시에 HOLD 성공**
- 결제(PAY) 동시 요청 → **좌석 상태 꼬임 및 결제 중복 생성**

본 과제에서는 Redis 기반 분산락을 적용하여 **좌석 단위 임계구역 진입을 1명으로 제한**함으로써, 상태 변경 로직의 원자성을 보장한다.

---

## 2. 예상 동시성 이슈 및 원인

### 2.1 좌석 HOLD 동시성 이슈
- 여러 스레드가 동일 좌석에 대해 동시에 HOLD 요청을 보낼 수 있다.
- 검증 → 상태 변경(hold) 사이 시간 간극에서 경쟁 상태(race condition)가 발생할 수 있다.

### 2.2 결제 PAY 동시성 이슈
- 동일 좌석(scheduleId + seatNo)에 대해 결제가 동시에 실행될 수 있다.
- 좌석 검증, 포인트 차감, reserve 반영, 결제 저장이 동시에 수행되면 중복 결제 가능성이 존재한다.

---

## 3. 분산락 설계

### 3.1 락 키 선정
- Lock Key: `lock:seat:{scheduleId}:{seatNo}`
- HOLD와 PAY 모두 동일한 락 키를 사용하여 좌석 단위로 자원을 보호한다.

### 3.2 락 범위 선정
- HOLD: 좌석 검증부터 tryHold 반영까지 전체
- PAY: 좌석 검증 → 포인트 차감 → reserve → 결제 저장 전체

---

## 4. 구현 상세

### 4.1 DistributedLockExecutor
- Redis `SETNX + TTL` 방식
- waitTimeout 동안 retryInterval로 재시도
- Lua 스크립트로 락 소유자 검증 후 해제

### 4.2 HOLD 적용
- 락 키: `lock:seat:{scheduleId}:{seatNo}`
- 락 획득 성공 시 1명만 HOLD 로직 진입

### 4.3 PAY 적용
- 락 키: `lock:seat:{scheduleId}:{seatNo}`
- 결제 로직 전체를 임계구역으로 보호

---

## 5. 테스트 전략 및 결과

### 5.1 SeatHoldDistributedLockIT
- 동일 좌석에 대해 10개 스레드 HOLD 요청
- 결과: success = 1, 좌석 상태 = HELD

### 5.2 PayDistributedLockIT
- HELD 상태 좌석에 대해 10개 스레드 PAY 요청
- 결과: success = 1, Payment row = 1, 좌석 상태 = RESERVED

---

## 6. 추가 고려사항 (확장 포인트)

### 6.1 TTL 및 재시도 전략
- LOCK_TTL: 비즈니스 로직 최대 수행 시간을 기준으로 설정
- waitTimeout / retryInterval: 사용자 체감 대기시간과 시스템 부하 균형 고려

### 6.2 장애 상황 대응
- Redis 장애 시 DB 락 또는 조건부 UPDATE 방식으로 fallback 가능
- 현재 과제에서는 Redis 단일 의존 구조로 구현

### 6.3 개선 여지
- HOLD/PAY별 에러 코드 분리
- Redisson 도입 검토
- 락 메트릭 수집 및 모니터링

---

## 7. 결론
Redis 기반 분산락을 좌석 단위로 적용하여 동시성 문제를 효과적으로 해결했다.
멀티스레드 통합 테스트를 통해 HOLD와 PAY 모두에서 단일 성공만 발생함을 검증했다.
