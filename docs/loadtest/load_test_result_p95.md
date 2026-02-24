# k6 Hold 부하테스트 결과 해석 (Queue Gate 포함)

실행 명령:

```bash
BASE_URL=http://localhost:8080 SCHEDULE_ID=1 READY_TIMEOUT_SEC=10 \
k6 run --summary-export=summary_hold.json loadtest/k6/hold.js
```

---

## 1. 이 테스트가 실제로 한 일 (시나리오 해석)

각 VU(가상 유저)는 아래 흐름을 반복했습니다.

1) `POST /api/v1/queue/token` 으로 **대기열 토큰 발급**
2) `GET /api/v1/queue/status` 를 **0.2초 간격 폴링**하여 `ready=true` 가 될 때까지 대기
3) `ready=true` 가 되면 `POST /api/v1/reservations/hold` 로 **좌석 선점(hold)** 요청
4) hold 응답은 **200(성공)** 또는 **409(이미 선점됨)** 둘 다 “정상 반응”으로 간주(check)

부하 패턴(Stages):

- 10s 동안 10 VU까지 증가
- 다음 20s 동안 30 VU
- 다음 20s 동안 50 VU
- 마지막 10s 동안 0으로 감소

총 실행시간은 rampdown/graceful 포함 약 **1m05s**.

---

## 2. 수치 “그대로” 

### 2.1 Thresholds (합격/불합격 기준)

- `http_req_duration p(95) < 800ms` ✅ **통과**
    - 실제 p95 = **7.09ms**
    - 의미: **서버가 요청을 처리하는 속도 자체는 매우 빠름**

- `http_req_failed rate < 1%` ❌ **실패**
    - 실제 rate = **3.01%**

여기서 중요한 포인트는 아래 2가지입니다.

1) **p95가 7ms로 매우 빠른데 실패율만 높다** → “성능” 문제가 아니라 “응답코드(특히 409)”가 많이 섞인 상황일 가능성이 큼
2) `http_req_failed`는 “내가 check에서 성공으로 인정했는지”와 별개로, **k6가 ‘실패 응답(기본: HTTP 4xx/5xx)’으로 분류하면 증가**함

---

### 2.2 Checks 

```
checks_succeeded: 99.85% (9143 / 9156)
checks_failed: 0.14% (13 / 9156)

✓ issue token 200
✓ status 200
✓ hold responded
✗ ready within timeout (13건 실패)
```

해석:

- 토큰 발급(200) OK
- 상태 조회(200) OK
- hold 응답도 “200 또는 409”로 정상 응답 OK
- 다만 **`ready within timeout`이 13번 실패**  
  → **10초 안에 ready=true가 되지 못한 케이스가 13회 있었다**는 의미

---

### 2.3 HTTP Metrics (요청/응답 지표)

- `http_reqs`: **9143** (초당 약 140 req/s)
- `http_req_duration p(95)`: **7.09ms**
- `iteration_duration p(95)`: **10s**
    - iteration p95가 10초인 것은, 스크립트가 `READY_TIMEOUT_SEC=10`으로 설정되어 있고  
      **ready 대기(폴링)가 최대 10초까지 걸릴 수 있기 때문**입니다.

---

## 3. 왜 `http_req_failed`가 3.01%로 나왔나? (핵심 원인)

### 결론: 대부분이 “409(좌석 선점 충돌)”이 `http_req_failed`로 집계된 것

k6의 `http_req_failed`는 기본적으로 **HTTP 상태코드가 4xx/5xx면 실패**로 집계합니다.

하지만 우리의 스크립트에서는:

- hold 응답이 `200`이면 성공
- hold 응답이 `409`여도 “정상 반응(동시성 경쟁 결과)”로 인정하도록 check를 작성했습니다.

즉,

- **check 관점**: 409도 정상(성공으로 인정)
- **k6 기본 지표 관점(http_req_failed)**: 409는 실패

그래서 **hold 경쟁이 많아질수록 http_req_failed가 자연스럽게 증가**합니다.

> 실제로 `http_req_failed 276 out of 9143`라는 숫자는, “전체 요청 중 276건이 4xx/5xx였다”는 뜻이며  
> 이 중 상당수(대부분)는 hold에서 발생한 **409**일 가능성이 매우 큽니다.

---

## 4. 그럼 ‘진짜 문제’는 뭐였나? 

### 원인 A) 409(좌석 충돌) — 정상적인 동시성 경쟁 결과
- VU가 증가하면 같은 좌석을 동시에 잡는 경쟁이 발생
- 이 시스템은 경쟁 상황에서 409로 거절하는 것이 정상 동작
- 다만 k6 기본 지표에서는 실패로 집계되어 threshold(1%)를 초과함

➡️ 해결(측정 관점): **409를 ‘기대 응답’으로 처리**하거나, 실패율 기준을 별도로 정의해야 함

---

### 원인 B) ready timeout 13건 — Queue Gate(permit 처리율) 병목 징후
`ready within timeout`이 실패했다는 건:

- 토큰을 받았지만,
- `READY_TIMEOUT_SEC=10` 안에 permit을 받아 `ready=true`가 되지 못했다는 뜻입니다.

현재 설정값(사용자가 공유한 application.yml):

- `queue.permit.batch-size = 1`
- `queue.permit.scheduler.delay-ms = 200`

이 조합이면 permit 발급 처리량이 대략:

- 200ms마다 1명 처리 → **초당 약 5명 수준**

부하가 50 VU까지 상승할 때 순간적으로 대기열이 쌓이면,
일부 VU가 10초 안에 ready를 못 받고 timeout에 걸릴 수 있습니다.

➡️ 해결(시스템 관점):
- batch-size 증가(예: 1 → 10)
- delay-ms 감소(예: 200 → 100)
- 또는 READY_TIMEOUT 증가(예: 10s → 30s)로 UX 타임아웃 완화

---