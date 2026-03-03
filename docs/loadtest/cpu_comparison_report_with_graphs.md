# CPU/Memory 스펙별 k6 Hold 부하 테스트 보고서 (그래프 포함)

실험 공통 조건
- 대상: `POST /api/v1/reservations/hold` (Queue Gate 포함)
- 시나리오: token 발급 → status 폴링(ready) → hold
- Max VU: 50 (ramping-vus: 10→30→50→0)
- READY_TIMEOUT_SEC: 10
- 성공 판정(check): hold는 200 또는 409를 정상으로 인정
- 주의: `http_req_failed`는 k6 기본 지표로 **4xx(예: 409)** 도 실패로 잡힙니다. 따라서 “실제 장애율”과 동일하지 않습니다.

---

## 1) 스펙별 결과 요약표

| 스펙 | p95(http_req_duration) | http_req_failed | ready timeout(건) | req/s(http_reqs) | iter/s |
|---|---:|---:|---:|---:|---:|
| 0.5 CPU / 512MB | 300.42ms | 6.33% | 135 | 105.19 | 9.08 |
| 1 CPU / 1GB | 7.12ms | 5.73% | 165 | 147.79 | 11.37 |
| 2 CPU / 2GB | 9.46ms | 3.81% | 154 | 140.12 | 8.08 |

> 핵심: **p95는 0.5CPU에서만 급격히 악화**되었고, 1CPU 이상에서는 매우 낮은(ms 단위) 수준으로 안정적입니다.  
> 반면 ready timeout은 모든 스펙에서 발생하여, “CPU만의 문제”가 아니라 **Queue Gate(permit 처리율/timeout 정책)** 영향이 강합니다.

---

## 2) 그래프

### 2.1 p95 응답시간 (ms)
![](../../../../../항해99-부하테스트/1)%20CPU별%20비교%20그래프%20버전%20보고서/cpu_p95_ms.png)

### 2.2 http_req_failed (%)
![](../../../../../항해99-부하테스트/1)%20CPU별%20비교%20그래프%20버전%20보고서/cpu_http_req_failed_pct.png)

### 2.3 ready timeout (건)
![](../../../../../항해99-부하테스트/1)%20CPU별%20비교%20그래프%20버전%20보고서/cpu_ready_timeout_count.png)

### 2.4 처리량: req/s
![](../../../../../항해99-부하테스트/1)%20CPU별%20비교%20그래프%20버전%20보고서/cpu_rps.png)

### 2.5 처리량: iter/s
![](../../../../../항해99-부하테스트/1)%20CPU별%20비교%20그래프%20버전%20보고서/cpu_iters_per_s.png)

---

## 3) 해석 및 원인 결론

### 결론 A — 0.5 CPU에서는 “지연이 크게 증가”
- 0.5CPU에서 p95가 300ms로 급등했습니다.
- 이는 CPU/GC/스레드 스케줄링 여유가 줄어들어, **queue/status 폴링 + hold 처리**가 밀리면서 tail latency가 커진 패턴과 일치합니다.
- 따라서 **최소 배포 스펙** 관점에서 0.5CPU는 리스크가 큽니다.

### 결론 B — 1 CPU 이상에서는 “API 처리 성능”은 충분
- 1CPU/2CPU에서 p95가 7~9ms로 매우 낮습니다.
- 즉, 애플리케이션이 요청을 처리하는 속도 자체는 충분합니다.

### 결론 C — ready timeout은 스펙을 올려도 남는다 → Queue Gate 병목
- 1CPU에서 오히려 ready timeout이 165건으로 증가했습니다(변동 폭은 있지만 ‘사라지지 않음’이 중요).
- 이는 ready 통과가 CPU가 아니라 **permit 발급 처리율(batch-size/delay)** 과 **READY_TIMEOUT(10s)** 에 의해 제한되기 때문입니다.
- 현재 설정(사용자 공유):
  - `batch-size=1`
  - `delay-ms=200` → 최대 처리량이 초당 약 5명 수준
- VU가 50까지 증가하면 permit 대기열이 순간적으로 쌓이며, 일부 요청은 10초 안에 ready를 못 받고 timeout이 발생할 수 있습니다.

### 결론 D — http_req_failed는 “409 좌석 충돌 + timeout 영향”으로 과대 측정
- k6 기본 지표 `http_req_failed`는 409를 실패로 집계합니다.
- 하지만 본 시스템에서 409는 동시성 경쟁 상황에서 정상적인 거절 응답입니다.
- 따라서 “실제 장애율”은 `ready timeout` + 5xx/네트워크 오류 중심으로 따로 정의하는 것이 정확합니다.

---

## 4) 배포 스펙 제안(현재 실험 기준)

- **권장 최소 스펙:** 1 CPU / 1GB
  - 0.5CPU에서 p95가 크게 악화(300ms) → tail latency 리스크
  - 1CPU 이상에서는 p95가 ms 단위로 안정

- **스펙 업(2CPU)만으로는 ready timeout이 해결되지 않음**
  - 큐 게이트 병목은 설정/알고리즘 개선이 필요

---

## 5) 다음 실험(점수 잘 나오는 확장)

1) READY_TIMEOUT_SEC=30으로 재실험  
   - 목적: 사용자 체감(타임아웃) 완화 효과 측정
2) permit 처리율 튜닝 후 재실험  
   - `batch-size: 1 → 10`
   - `delay-ms: 200 → 100`
3) “진짜 실패율” 지표 분리  
   - 409를 기대 응답으로 처리하거나,
   - ready timeout 비율을 별도 metric으로 계산

---

### 첨부 파일
- `summary_0.5cpu_512m.json`
- `summary_1cpu_1g.json`
- `summary_2cpu_2g.json`
