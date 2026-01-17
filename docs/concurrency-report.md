# 콘서트 예약 서비스 동시성 이슈 대응 보고서

## 1. 문제 상황(예상 동시성 이슈)

### (1) 같은 좌석에 대해 동시에 예약 요청 → 중복 예약 발생
- 여러 사용자가 동일 좌석을 동시에 HOLD/예약 시도할 경우, 단순 `SELECT → UPDATE` 방식은 레이스 컨디션 발생 가능.

### (2) 잔액 차감 중 충돌 발생 → 음수 잔액
- 동일 유저가 결제를 중복 요청하거나, 여러 요청이 동시에 포인트 차감을 수행하면 잔액이 음수로 내려갈 위험.

### (3) 예약 후 결제 지연 → 임시 배정 해제 로직 부정확
- HOLD 만료 후 해제가 누락되면 좌석이 영구적으로 HELD 상태로 남거나,
  반대로 해제 타이밍이 부정확하면 정상 결제 중인 좌석이 풀릴 수 있음.


---

## 2. 해결 전략

## 2-1. 좌석 임시 배정(HOLD) 동시성 제어
- DB 조건부 UPDATE(`tryHold`)로 원자적 선점
- `AVAILABLE` 또는 `만료된 HELD`만 업데이트되도록 조건 설정
- UPDATE row count(1/0)로 성공 여부 판단 → DB가 동시성 보장

**적용 기술**
- 조건부 UPDATE (JPQL)

**테스트**
- `SeatHoldConcurrencyIT`: 동시에 같은 좌석 hold → 1명만 성공


## 2-2. 잔액 차감 동시성 제어(음수 방지)
- `UserPoint`에 `@Version`을 적용한 낙관적 락 기반 처리
- 동시 업데이트 충돌(OptimisticLockException)은 정상 케이스로 간주하고 재시도 정책 적용
- 잔액 부족은 도메인 검증으로 차단(`INSUFFICIENT_BALANCE`) → 음수 잔액 방지

**적용 기술**
- 낙관적 락(@Version) + 재시도(TransactionTemplate)

**테스트**
- `PointSpendConcurrencyIT`
    - 초기 잔액 50,000
    - 10개 스레드가 동시에 10,000씩 spend
    - 기대: 성공 5 / 실패 5, 최종 잔액 0, 음수 불가


## 2-3. 배정 타임아웃 해제 스케줄러
- 만료된 HOLD를 주기적으로 `AVAILABLE`로 되돌림
- 스케줄러는 `ReleaseExpiredHoldsUseCase.releaseExpired()` 호출
- 테스트는 스케줄 시간을 기다리지 않고 스케줄러 메서드 직접 호출로 검증

**적용 기술**
- 스케줄러(@Scheduled)
- 만료 해제: 조건부 UPDATE (releaseExpiredHolds)

**테스트**
- `SeatHoldExpirySchedulerIT`
    - 만료된 HOLD 좌석 생성 → 스케줄러 실행 → AVAILABLE 전환 확인


---

## 3. 테스트 결과

### 3-1. 좌석 hold 동시성
- 결과: 동시에 같은 좌석 요청 시 1명만 성공

### 3-2. 잔액 차감 동시성
- 결과: 동시 spend에서도 음수 잔액 발생 없음
- 성공/실패 건수 기대값과 일치

### 3-3. 만료 해제 스케줄러
- 결과: 만료된 HOLD가 스케줄러 실행으로 AVAILABLE로 전환

