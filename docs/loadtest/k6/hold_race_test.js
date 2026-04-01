import http from 'k6/http';
import { check, sleep } from 'k6';
import { Counter, Rate, Trend } from 'k6/metrics';

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const SCHEDULE_ID = Number(__ENV.SCHEDULE_ID || '1');
const READY_TIMEOUT_SEC = parseInt(__ENV.READY_TIMEOUT_SEC || '10', 10);
const TARGET_SEAT_NO = Number(__ENV.TARGET_SEAT_NO || '1');

// -----------------------------
// Custom Metrics
// -----------------------------
export const expected_error = new Counter('expected_error');
export const unexpected_error = new Counter('unexpected_error');
export const unexpected_error_rate = new Rate('unexpected_error_rate');

export const ready_timeout_count = new Counter('ready_timeout_count');

export const hold_success_count = new Counter('hold_success_count');
export const hold_conflict_count = new Counter('hold_conflict_count');

export const token_req_ms = new Trend('token_req_ms');
export const queue_wait_ms = new Trend('queue_wait_ms');
export const queue_status_ms = new Trend('queue_status_ms');
export const hold_req_ms = new Trend('hold_req_ms');

// -----------------------------
// Options
// -----------------------------
export const options = {
    scenarios: {
        same_seat_race_50vu: {
            executor: 'per-vu-iterations',
            vus: 50,
            iterations: 1,
            maxDuration: '1m',
        },
    },
    thresholds: {
        unexpected_error_rate: ['rate<0.01'],
        ready_timeout_count: ['count<1'],
        hold_success_count: ['count>=1'],
    },
};

// -----------------------------
// Helpers
// -----------------------------
function classifyStatus(status) {
    const EXPECTED = new Set([409]);
    if (EXPECTED.has(status)) return 'expected';
    if (status >= 400) return 'unexpected';
    return 'ok';
}

function markResult(res, kind) {
    const classified = classifyStatus(res.status);

    if (classified === 'expected') {
        expected_error.add(1);
        unexpected_error_rate.add(false);
    } else if (classified === 'unexpected') {
        unexpected_error.add(1);
        unexpected_error_rate.add(true);
    } else {
        unexpected_error_rate.add(false);
    }

    if (kind === 'token') token_req_ms.add(res.timings.duration);
    if (kind === 'queue_status') queue_status_ms.add(res.timings.duration);
    if (kind === 'hold') hold_req_ms.add(res.timings.duration);

    return classified;
}

function issueToken(userUuid) {
    const url =
        `${BASE_URL}/api/v1/queue/token` +
        `?userUuid=${encodeURIComponent(userUuid)}` +
        `&scheduleId=${SCHEDULE_ID}`;

    const res = http.post(url);

    check(res, {
        'issue token status is 200': (r) => r.status === 200,
    });

    markResult(res, 'token');

    let queueToken = null;
    try {
        const body = res.json();
        queueToken = body.queueToken;
    } catch (e) {
        queueToken = null;
    }

    return queueToken;
}

function waitReady(token) {
    const start = Date.now();
    const deadline = start + READY_TIMEOUT_SEC * 1000;

    while (Date.now() < deadline) {
        const res = http.get(`${BASE_URL}/api/v1/queue/status`, {
            headers: { 'X-QUEUE-TOKEN': token },
        });

        check(res, {
            'queue status is 200': (r) => r.status === 200,
        });

        markResult(res, 'queue_status');

        let ready = false;
        try {
            const body = res.json();
            ready = body.ready === true;
        } catch (e) {
            ready = false;
        }

        if (ready) {
            queue_wait_ms.add(Date.now() - start);
            return true;
        }

        sleep(0.1);
    }

    ready_timeout_count.add(1);
    return false;
}

function buildUniqueUserId() {
    return __VU * 1000000 + __ITER + 1;
}

function buildUniqueUserUuid() {
    return `race-user-${__VU}-${__ITER}-${Date.now()}`;
}

// -----------------------------
// Main
// -----------------------------
export default function () {
    const userId = buildUniqueUserId();
    const userUuid = buildUniqueUserUuid();

    const token = issueToken(userUuid);

    check(token, {
        'queue token exists': (t) => !!t,
    });

    if (!token) {
        return;
    }

    const isReady = waitReady(token);

    if (!isReady) {
        check(null, {
            'ready within timeout': () => false,
        });
        return;
    }

    const payload = JSON.stringify({
        userId,
        scheduleId: SCHEDULE_ID,
        seatNo: TARGET_SEAT_NO,
    });

    const res = http.post(
        `${BASE_URL}/api/v1/reservations/hold`,
        payload,
        {
            headers: {
                'Content-Type': 'application/json',
                'X-QUEUE-TOKEN': token,
            },
        }
    );

    markResult(res, 'hold');

    if (res.status === 200 || res.status === 201) {
        hold_success_count.add(1);
    } else if (res.status === 409) {
        hold_conflict_count.add(1);
    }

    check(res, {
        'hold is success or expected conflict': (r) =>
            r.status === 200 || r.status === 201 || r.status === 409,
    });
}

/*
이건 정합성 검증용이야.

특징:

50명이 거의 동시에 1번씩만 요청
전부 같은 좌석 요청
좌석 중복 선점 방지 로직 검증
기대 결과:
성공: 1건 또는 매우 소수
나머지: 409
예상 못한 500 같은 에러 없음

실행예시
BASE_URL=http://localhost:8080 \
SCHEDULE_ID=1 \
READY_TIMEOUT_SEC=10 \
TARGET_SEAT_NO=1 \
k6 run hold_race_test.js

 */