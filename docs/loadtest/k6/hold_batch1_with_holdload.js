import http from 'k6/http';
import { check, sleep } from 'k6';
import { Counter, Rate, Trend } from 'k6/metrics';

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const SCHEDULE_ID = __ENV.SCHEDULE_ID || '1';
const READY_TIMEOUT_SEC = parseInt(__ENV.READY_TIMEOUT_SEC || '10', 10);

// --- Custom Metrics ---
export const expected_error = new Counter('expected_error');
export const unexpected_error = new Counter('unexpected_error');
export const unexpected_error_rate = new Rate('unexpected_error_rate');

export const ready_timeout_count = new Counter('ready_timeout_count');

export const token_req_ms = new Trend('token_req_ms');
export const queue_wait_ms = new Trend('queue_wait_ms');
export const hold_req_ms = new Trend('hold_req_ms');
export const queue_status_ms = new Trend('queue_status_ms');

// --- Options ---
export const options = {
    scenarios: {
        // baseline
        baseline_50vu_10m: {
            executor: 'constant-vus',
            vus: 50,
            duration: '10m',
            gracefulStop: '10s',
        },

        // 추가 부하 (병목 유도)
        hold_load: {
            executor: 'ramping-vus',
            startVUs: 0,
            stages: [
                { duration: '10s', target: 10 },
                { duration: '20s', target: 30 },
                { duration: '20s', target: 50 },
                { duration: '10s', target: 0 },
            ],
            gracefulRampDown: '10s',
        },
    },

    thresholds: {
        unexpected_error_rate: ['rate<0.01'],
        http_req_duration: ['p(95)<800'],
        hold_req_ms: ['p(95)<800'],

        // 일부러 깨지는 케이스 (병목 확인용)
        ready_timeout_count: ['count<1'],
    },
};

// --- 상태 분류 ---
function classifyStatus(status) {
    const EXPECTED = new Set([409]);
    if (EXPECTED.has(status)) return 'expected';
    if (status >= 400) return 'unexpected';
    return 'ok';
}

// --- 결과 기록 ---
function markResult(res, kind) {
    const t = classifyStatus(res.status);

    if (t === 'expected') {
        expected_error.add(1);
        unexpected_error_rate.add(false);
    } else if (t === 'unexpected') {
        unexpected_error.add(1);
        unexpected_error_rate.add(true);
    } else {
        unexpected_error_rate.add(false);
    }

    if (kind === 'token') token_req_ms.add(res.timings.duration);
    if (kind === 'queue_status') queue_status_ms.add(res.timings.duration);
    if (kind === 'hold') hold_req_ms.add(res.timings.duration);

    return t;
}

// --- 토큰 발급 ---
function issueToken(userUuid) {
    const url = `${BASE_URL}/api/v1/queue/token?userUuid=${encodeURIComponent(userUuid)}&scheduleId=${SCHEDULE_ID}`;
    const res = http.post(url);

    check(res, { 'issue token 200': (r) => r.status === 200 });

    markResult(res, 'token');

    const body = res.json();
    return body.queueToken;
}

// --- ready 대기 ---
function waitReady(token) {
    const start = Date.now();
    const deadline = start + READY_TIMEOUT_SEC * 1000;

    while (Date.now() < deadline) {
        const res = http.get(`${BASE_URL}/api/v1/queue/status`, {
            headers: { 'X-QUEUE-TOKEN': token },
        });

        check(res, { 'status 200': (r) => r.status === 200 });

        markResult(res, 'queue_status');

        const body = res.json();
        if (body.ready === true) {
            queue_wait_ms.add(Date.now() - start);
            return true;
        }

        sleep(0.2);
    }

    ready_timeout_count.add(1);
    return false;
}

// --- 메인 흐름 ---
export default function () {
    const userUuid = `user-${__VU}-${__ITER}`;

    const token = issueToken(userUuid);

    const ok = waitReady(token);
    if (!ok) {
        check(null, { 'ready within timeout': () => false });
        return;
    }

    // 좌석 분산 (병목 테스트에서는 중요하지 않음)
    const seatNo = ((__VU * 1000 + __ITER) % 50) + 1;

    const res = http.post(
        `${BASE_URL}/api/v1/reservations/hold`,
        JSON.stringify({
            userId: 1,
            scheduleId: Number(SCHEDULE_ID),
            seatNo,
        }),
        {
            headers: {
                'Content-Type': 'application/json',
                'X-QUEUE-TOKEN': token,
            },
        }
    );

    markResult(res, 'hold');

    check(res, {
        'hold responded ok/expected': (r) =>
            r.status === 200 || r.status === 409,
    });

    sleep(0.1);
}