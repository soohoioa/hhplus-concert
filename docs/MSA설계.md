# MSA 설계

---

## 1. 도메인별 배포 단위(서비스 경계) 설계

### 1) Concert Service
- 책임: 콘서트/스케줄 정보 조회
- 특징: 읽기 중심, 상태 변경 없음
- 주요 리소스: Concert, ConcertSchedule

### 2) Reservation Service
- 책임: 좌석 상태 관리(AVAILABLE / HELD / RESERVED)
- 특징: 동시성 제어 및 상태 전이 책임
- 주요 리소스: ScheduleSeat

### 3) Payment Service
- 책임: 결제 처리, 결제 완료 이벤트 발행
- 특징: 결제 흐름의 오케스트레이터 역할
- 주요 리소스: Payment

### 4) Point Service
- 책임: 포인트 충전/차감, 잔액 관리
- 특징: 원장성 데이터, 동시성 제어 필요
- 주요 리소스: UserPoint

### 5) Queue Service
- 책임: 대기열 토큰 발급 및 검증
- 특징: 진입 제어 목적, 결제 트랜잭션과 직접적 결합은 낮음

### 6) Ranking Service
- 책임: 매진 랭킹 기록
- 특징: 통계성 데이터, 핵심 트랜잭션과 분리 가능

### 7) DataPlatform Integration Service
- 책임: 외부 데이터 플랫폼 전송(mock API)
- 특징: 외부 I/O, 실패 가능성이 높은 영역

---

## 2. 중심 트랜잭션 정의: 결제(pay)

### 2.1 모놀리식 환경의 결제 트랜잭션

모놀리식 환경에서는 다음 작업이 하나의 DB 트랜잭션으로 처리된다.

1. 좌석 상태 검증 (HELD 여부, 사용자 일치, 만료 시간 확인)
2. 포인트 차감
3. 좌석 예약 확정 (HELD -> RESERVED)
4. 결제 정보 저장
5. 매진 여부 판단 및 랭킹 기록
6. 예약 정보를 외부 데이터 플랫폼에 전송

### 2.2 MSA 분리 이후의 변화

MSA 환경에서는 위 단계들이 서로 다른 서비스와 DB에 분산되며, 단일 DB 트랜잭션으로 묶을 수 없게 된다. -> 이는 분산 트랜잭션 문제를 발생시킨다.

---

## 3. 트랜잭션 진단(Transaction Diagnosis)

### 3.1 원자성(Atomicity) 문제

결제 성공, 포인트 차감 실패, 포인트 차감 성공, 좌석 확정 실패 와 같은 부분 성공 상태가 발생할 수 있고, 이로 인해 데이터 불일치 상태가 발생할 수 있다.

---

### 3.2 일관성(Consistency) 문제

MSA 환경에서는 즉시 일관성(Strong Consistency)을 보장하기 어렵다.
- 결제 완료 후에도 좌석 상태, 외부 플랫폼 반영은 지연될 수 있어 최종 일관성(Eventual Consistency)을 전제로 설계해야 한다.

---

### 3.3 장애 및 재시도 문제

- 네트워크 오류, 외부 API 장애, 서비스 재시작
- 중복 요청/중복 이벤트로 인한 중복 처리 위험
- 외부 전송 실패로 인한 데이터 유실 가능성

---

## 4. 해결 방안(서비스 설계)

### 4.1 Saga 패턴 (오케스트레이션 방식)

Payment Service가 결제 흐름의 오케스트레이터 역할을 수행한다.

#### 정상 흐름 예시

1. Payment Service: 결제 요청 수신
2. Reservation Service: 좌석 확정 요청
3. Point Service: 포인트 차감
4. Payment Service: 결제 완료 처리
5. PaymentCompleted 이벤트 발행
6. DataPlatform Service: 외부 플랫폼 전송
7. Ranking Service: 매진 랭킹 기록

---

### 4.2 보상 트랜잭션(Compensation)

롤백 대신 보상 트랜잭션을 수행한다.

- 좌석 확정 후 포인트 차감 실패 -> 좌석 확정 취소(RESERVED → AVAILABLE)
- 포인트 차감 후 좌석 확정 실패 -> 포인트 복구 처리

---

### 4.3 멱등성(Idempotency) 설계

중복 요청 및 이벤트 재처리를 대비한다.

- Payment Service: paymentId 기반 중복 결제 방지
- Point Service: paymentId 기준 포인트 차감 중복 방지
- Reservation Service: reservationId 기준 좌석 확정 중복 방지

---

### 4.4 이벤트 기반 통합

결제 완료 후 이벤트를 발행하여 외부 연동을 처리한다.

- 결제 트랜잭션과 외부 I/O 분리
- 트랜잭션 커밋 이후 이벤트 처리
- 관심사 분리 및 결합도 감소

---

### 4.5 Outbox 패턴(확장 설계)

- 결제 트랜잭션 내에서 이벤트를 Outbox 테이블에 저장
- 별도 프로세서가 Outbox를 읽어 외부 전송
- 실패 시 재시도 및 상태 관리 가능

**현재는 결제 이벤트를 Outbox에 적재하는 단계까지만 적용했습니다.**
**이는 결제 트랜잭션과 이벤트 기록의 원자성을 확보하기 위함이며, 추후 Kafka Producer 또는 Outbox Processor를 추가해 확장할 계획입니다.**

---

## 5. 결론

결제를 중심으로 MSA로 분리할 경우, 단일 트랜잭션의 원자성은 유지할 수 없다.  

따라서 결제 트랜잭션을 분산된 로컬 트랜잭션들의 조합으로 재정의하고, Saga 패턴, 보상 트랜잭션, 멱등성 설계, 이벤트 기반 통합, (선택적) Outbox 패턴을 통해 최종 일관성과 장애 복구 가능성을 확보하는 방향으로 설계한다.

---
