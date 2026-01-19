# Redis 기반 조회 캐시 전략 적용 

## 1. 과제 목표
- 조회가 오래 걸리거나 자주 변하지 않는 데이터를 Redis 캐시로 처리
- DB 부하 감소 및 응답 속도 개선
- 캐시 전략 설계 및 테스트를 통한 성능 개선 검증

---

## 2. 캐시 적용 대상 선정

### 2.1 공연 일정 조회
- 조회 빈도 높음
- 데이터 변경 빈도 낮음
- 캐시 적합성 매우 높음

### 2.2 좌석 조회
- 조회 빈도 매우 높음
- HOLD/RESERVE 시 상태 변경
- 캐시 적용 가능하나 Evict 전략 필수

---

## 3. 캐시 전략 설계

### 3.1 공연 일정 조회 캐시
- Cache Name: concert:schedules
- TTL: 10분
- Evict 전략: 없음 (관리자 변경 시 수동 Evict)

### 3.2 좌석 조회 캐시
- Cache Name: concert:available-seats
- TTL: 15초
- Evict 전략: HOLD 성공 시 즉시 캐시 삭제

---

## 4. Redis 설정 및 직렬화 전략

### 4.1 Serializer
- GenericJackson2JsonRedisSerializer 사용
- 타입 정보 포함(DefaultTyping.NON_FINAL)

### 4.2 ObjectMapper 설정
- JavaTimeModule 등록
- LocalDateTime 직렬화 지원
- Timestamp 비활성화

### 4.3 Key Prefix
- hhplus:{cacheName}::{key}
- 서비스 단위 캐시 충돌 방지

---

## 5. 테스트 전략

### 5.1 캐시 HIT 검증
- 동일 요청 2회 호출
- Port 호출 횟수 1회 검증(Mockito Spy)

### 5.2 캐시 Evict 검증
- HOLD 성공 후 캐시 삭제
- 이후 조회 시 DB 재조회 확인

---

## 6. 결론
- Redis 캐시는 조회 성능 개선에 매우 효과적
- TTL + Evict 전략을 함께 사용해야 데이터 정합성 유지 가능
- 캐시 적용은 단순 성능 최적화가 아닌 시스템 안정성 확보 수단이다
