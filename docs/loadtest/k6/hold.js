import http from 'k6/http';
import { check, sleep } from 'k6';

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const SCHEDULE_ID = __ENV.SCHEDULE_ID || '1';
const READY_TIMEOUT_SEC = parseInt(__ENV.READY_TIMEOUT_SEC || '10', 10);

export const options = {
  scenarios: {
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
    http_req_failed: ['rate<0.01'],
    http_req_duration: ['p(95)<800'],
  },
};

function issueToken(userUuid) {
  const url = `${BASE_URL}/api/v1/queue/token?userUuid=${encodeURIComponent(userUuid)}&scheduleId=${SCHEDULE_ID}`;
  const res = http.post(url);
  check(res, { 'issue token 200': (r) => r.status === 200 });
  const body = res.json();
  return body.queueToken;
}

function waitReady(token) {
  const deadline = Date.now() + READY_TIMEOUT_SEC * 1000;

  while (Date.now() < deadline) {
    const res = http.get(`${BASE_URL}/api/v1/queue/status`, {
      headers: { 'X-QUEUE-TOKEN': token },
    });
    check(res, { 'status 200': (r) => r.status === 200 });
    const body = res.json();
    if (body.ready === true) return true;
    sleep(0.2);
  }
  return false;
}

export default function () {
  const userUuid = `user-${__VU}-${__ITER}`;
  const token = issueToken(userUuid);

  const ok = waitReady(token);
  if (!ok) {
    check(null, { 'ready within timeout': () => false });
    return;
  }

  // schedule_id=1 좌석이 50개면 1~50으로 분산
  const seatNo = ((__VU * 1000 + __ITER) % 50) + 1;

  const res = http.post(
    `${BASE_URL}/api/v1/reservations/hold`,
    JSON.stringify({ userId: 1, scheduleId: Number(SCHEDULE_ID), seatNo }),
    { headers: { 'Content-Type': 'application/json', 'X-QUEUE-TOKEN': token } }
  );

  check(res, {
    'hold responded': (r) => r.status === 200 || r.status === 409,
  });

  sleep(0.1);
}

