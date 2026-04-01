import http from 'k6/http';
import { check, sleep } from 'k6';
import { Counter, Rate, Trend } from 'k6/metrics';

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const SCHEDULE_ID = Number(__ENV.SCHEDULE_ID || '1');
const READY_TIMEOUT_SEC = parseInt(__ENV.READY_TIMEOUT_SEC || '10', 10);
const SEAT_COUNT = Number(__ENV.SEAT_COUNT || '50');

// -----------------------------
// Custom Metrics
// -----------------------------
export const expected_error = new Counter('expected_error');             // 409 등 기대된 비즈니스 실패
export const unexpected_error = new Counter('unexpected_error');         // 예상 못한 4xx/5xx
export const unexpected_error_rate = new Rate('unexpected_error_rate');  // 진짜 장애율

export const ready_timeout_count = new Counter('ready_timeout_count');

export const token_req_ms = new Trend('token_req_ms');
export const queue_wait_ms = new Trend('queue_wait_ms');
export const queue_status_ms = new Trend('queue_status_ms');
export const hold_req_ms = new Trend('hold_req_ms');

// -----------------------------
// Options
// -----------------------------
export const options = {
    scenarios: {
        baseline_50vu_10m: {
            executor: 'constant-vus',
            vus: 50,
            duration: '10m',
            gracefulStop: '10s',
        },
    },
    thresholds: {
        unexpected_error_rate: ['rate<0.01'],
        hold_req_ms: ['p(95)<120'],
        ready_timeout_count: ['count<1'],
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

        sleep(0.2);
    }

    ready_timeout_count.add(1);
    return false;
}

function buildUniqueUserId() {
    return __VU * 1000000 + __ITER + 1;
}

function buildUniqueUserUuid() {
    return `user-${__VU}-${__ITER}-${Date.now()}`;
}

function pickDistributedSeatNo() {
    return ((__VU * 1000 + __ITER) % SEAT_COUNT) + 1;
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

    const seatNo = pickDistributedSeatNo();

    const payload = JSON.stringify({
        userId,
        scheduleId: SCHEDULE_ID,
        seatNo,
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

    check(res, {
        'hold is success or expected conflict': (r) =>
            r.status === 200 || r.status === 201 || r.status === 409,
    });

    sleep(0.1);
}

/*
50 VU 고정
10분 유지
seat를 분산해서 일반적인 부하 상황 재현
hold_req_ms로 hold API만 따로 측정
hold_req_ms p(95)<120을 직접 검증

BASE_URL=http://localhost:8080 \
SCHEDULE_ID=1 \
READY_TIMEOUT_SEC=10 \
SEAT_COUNT=50 \
k6 run hold_perf_test.js
 */