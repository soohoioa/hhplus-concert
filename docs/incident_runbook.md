# 장애 대응 매뉴얼 (Incident Runbook)
## Hold API / Queue Gate 기반 콘서트 예매 시스템

> 목적: 부하 테스트 결과를 근거로, **예측 가능한 병목(permit/DB/리소스)** 및 **예상 못한 장애(의존성, 네트워크, 설정, 배포)** 상황에서 운영자가 즉시 대응할 수 있도록 절차를 문서화한다.

---

## 0. 시스템 요약(한 줄)
대기열 토큰(Queue Token) 발급 → permit 스케줄러로 ready 전환 → 좌석 hold(동시성 경합) → (확장) 결제/Outbox/Kafka 발행.

---

## 1. 핵심 엔드포인트/의존성
### 주요 API
- `POST /api/v1/queue/token?userUuid=...&scheduleId=...`
- `GET /api/v1/queue/status` (Header: `X-QUEUE-TOKEN`)
- `POST /api/v1/reservations/hold` (Header: `X-QUEUE-TOKEN`)

### 핵심 컴포넌트
- Redis: 대기열/permit 상태 저장
- MySQL: `schedule_seat` 좌석 상태/hold 만료/유저 정보
- App: permit scheduler(배치/딜레이 정책), hold 트랜잭션
- (확장) Kafka/ZK: 이벤트 발행/전송 (outbox 패턴)

---

## 2. 정상 동작 기준(SLO 제안)
> 부하 테스트 데이터를 바탕으로 운영 기준을 “측정 가능한 수치”로 정의한다.

### 권장 SLO (초기)
- Hold API p95: **< 800ms**
- Ready timeout 비율: **< 1%**
- 5xx 비율: **< 0.5%**
- 409(좌석 충돌): **정상(경쟁 반영)** → 장애 지표에서 제외 (별도 지표로 관찰)

---

## 3. 부하 테스트 기반 병목 결론(운영 관점 요약)
### 3-1. CPU/Memory 스펙 병목
- 0.5 CPU / 512MB: p95가 **300ms 수준까지 상승** → tail latency 리스크
- 1 CPU / 1GB 이상: p95가 **ms 단위로 안정**

✅ 운영 결론: 최소 배포 스펙은 **1 CPU / 1GB** 권장

### 3-2. Queue Gate 병목 (permit)
- `batch-size=1`, `delay=200ms` → 초당 약 5명 수준으로 ready 처리
- VU 증가 시 **ready timeout**이 늘어남 → CPU를 올려도 완전 해소되지 않음

✅ 운영 결론: 트래픽 급증 시 **permit 처리율/timeout 정책**이 1차 병목

### 3-3. 409 좌석 선점 충돌
- 409는 **동시성 경쟁에서 정상적인 거절 응답**
- k6 `http_req_failed`에 포함되어 “실패율”이 과대 측정됨

✅ 운영 결론: 장애 판단은 409가 아니라 **ready timeout, 5xx, latency**로 한다

---

## 4. 모니터링/알림(필수)
### 4-1. 알림 조건(우선순위)
P0 (즉시 대응)
- Hold API 5xx > 1% (5분 평균)
- Hold API p95 > 2s (5분 평균)
- `/queue/status` 5xx 발생 또는 응답 불가
- MySQL connection 실패/timeout 발생

P1 (원인 분석/조치)
- Ready timeout 비율 > 3%
- Redis latency 상승(예: p95 > 20ms)
- DB 커넥션 풀 고갈 징후(active=maximum)

### 4-2. 확인할 지표(최소)
- App: 요청량(RPS), p95, 5xx, thread/CPU, GC, Hikari(active/idle)
- Redis: CPU, 연결 수, latency, keyspace
- MySQL: connections, slow query, lock wait, CPU/IO
- Kafka(확장): consumer lag, produce error, outbox 적체량

---

## 5. 장애 유형별 대응 시나리오(Playbook)

### A) Ready timeout 급증(permit 병목)  ✅ 예측 가능
**증상**
- `/queue/status`에서 ready=true 전환이 느림
- k6에서 `ready within timeout` 실패 증가
- 사용자: “대기열에서 계속 대기”

**즉시 확인**
1) 현재 설정 확인 (batch-size, delay-ms, timeout)
2) Redis 상태 확인(대기열 키 증가 여부)
3) App CPU/스레드 포화 여부

**즉시 조치(순서대로)**
1) (운영 정책) **READY_TIMEOUT 상향** (예: 10s → 30s)
2) (처리율) **permit batch-size 상향** (1 → 10)
3) (반응성) **delay-ms 하향** (200 → 100)
4) (스케일) App CPU 1 → 2 확장(필요 시)

**주의**
- batch-size를 키우면 “동시에 ready=true”가 늘어 hold 경쟁이 증가할 수 있음 → 좌석 충돌(409) 상승은 정상

**사후 조치**
- peak 시간대 정책값(배치/딜레이/timeout) 별도 프로파일로 관리
- permit 처리율과 사용자 ETA 계산의 정합성 점검

---

### B) Hold API p95 급상승(리소스 부족) ✅ 예측 가능
**증상**
- p95가 수백 ms ~ 초 단위로 상승
- 사용자: “좌석 선택이 느림”

**즉시 확인**
1) 컨테이너 CPU/메모리 제한 확인 (`docker inspect`)
2) App CPU 사용률 / GC / thread wait
3) MySQL lock wait / slow query

**즉시 조치**
1) 최소 스펙 미만이면 즉시 상향 (0.5 → 1 CPU)
2) DB 커넥션 풀 점검(최대 3이면 병목 가능) → 상향 검토
3) hold 트랜잭션 내 쿼리 수/락 범위 점검

---

### C) MySQL 커넥션 풀 고갈(Hikari) ✅ 예측 가능
**증상**
- 응답 지연/timeout
- 로그에 connection timeout
- DB connections 급증

**즉시 확인**
- Hikari active/idle/waiting
- MySQL `SHOW PROCESSLIST`, `SHOW STATUS LIKE 'Threads_connected'`

**즉시 조치**
1) Hikari pool 상향(예: 3 → 10/20)
2) 슬로우 쿼리 확인 및 인덱스/쿼리 개선
3) 트래픽 급증 시 hold 요청 rate-limit 고려

---

### D) Redis 장애/latency 상승 ✅ 예측 가능
**증상**
- token/status API가 느려짐 또는 실패
- ready 전환 불가, timeout 급증

**즉시 확인**
- Redis CPU, latency, 연결 수
- key scan으로 특정 키 폭증 여부

**즉시 조치**
1) Redis 재시작/리소스 확장(가능 시)
2) permit 처리율 낮추고 timeout 상향(완화)
3) 임시로 “대기열 우회” 정책(운영 모드) 고려(최후 수단)

---

### E) Kafka/ZK 이슈(확장 시) ✅ 예측 가능 + 장애 파급 큼
**증상**
- outbox 적체, 이벤트 전송 지연
- producer send 실패 로그 증가

**즉시 조치**
1) outbox processor 재시도 정책/백오프 확인
2) consumer lag 및 broker 상태 확인
3) 장애 시 outbox는 DB에 남도록(유실 방지) 유지

---

### F) 예상 못한 장애: 배포/설정 실수(ENV, secret, profile) ⭐ BEST
**증상**
- 갑자기 401/403 증가(토큰/헤더 정책 변경)
- 특정 scheduleId만 실패(데이터 seed 문제)
- profile 잘못 적용(local/prod 혼동)

**즉시 조치**
1) 최근 배포 diff 확인(설정/환경변수)
2) health check API/로그 확인
3) 문제 설정 롤백 or 안전한 기본값 적용

---

### G) 예상 못한 장애: 네트워크/DNS/포트 충돌 ⭐ BEST
**증상**
- Redis/MySQL 연결 실패
- 컨테이너는 살아있는데 API timeout

**즉시 조치**
1) `docker ps`, `docker logs -f hhplus-app`
2) 컨테이너 간 통신 확인(ping/port)
3) 포트 매핑/방화벽/리버스 프록시 확인

---

## 6. 장애 발생 시 공통 체크리스트(5분 루틴)
1) 현재 증상 분류: **latency? 5xx? ready timeout? dependency?**
2) App 로그 확인: `docker logs -f hhplus-app`
3) 리소스 확인: `docker stats`
4) Redis/MySQL 상태 확인(연결/latency)
5) 최근 변경사항(배포/설정) 확인
6) 완화 조치(스케일/timeout/permit) 후 재측정

---

## 7. 운영 모드(완화 정책) 제안
트래픽 폭주 시 즉시 적용 가능한 운영 레버:
- READY_TIMEOUT 상향 (사용자 경험 개선)
- permit batch-size 상향 + delay 하향 (대기열 처리율 개선)
- App 스케일 업 (0.5→1→2 CPU)
- DB pool 상향(필요 시)
- (최후) hold 요청 rate-limit / per-user 제한

---

## 8. 부록: 빠른 명령어 모음
### 리소스 확인
```bash
docker stats --no-stream hhplus-app
docker inspect hhplus-app --format 'NanoCpus={{.HostConfig.NanoCpus}} Memory={{.HostConfig.Memory}} MemorySwap={{.HostConfig.MemorySwap}}'
```

### 좌석 초기화(테스트/장애 복구용)
```bash
docker exec -it mysql mysql -uapplication -papplication -Dhhplus -e "
UPDATE schedule_seat
SET status='AVAILABLE',
    hold_expires_at=NULL,
    hold_user_id=NULL,
    reserved_at=NULL,
    reserved_user_id=NULL
WHERE schedule_id=1;"
```

### Redis 키 확인(환경에 따라 패턴 조정)
```bash
docker exec -it redis redis-cli --scan --pattern "*queue*"
docker exec -it redis redis-cli --scan --pattern "*permit*"
```

---
