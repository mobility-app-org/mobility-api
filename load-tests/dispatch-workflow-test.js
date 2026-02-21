import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate } from 'k6/metrics';

// 커스텀 메트릭
const errorRate = new Rate('errors');

// 테스트 설정
export const options = {
  stages: [
    { duration: '30s', target: 10 },  // 30초 동안 10명의 VU로 증가
    { duration: '1m', target: 20 },   // 1분 동안 20명의 VU 유지
    { duration: '30s', target: 50 },  // 30초 동안 50명의 VU로 증가
    { duration: '1m', target: 50 },   // 1분 동안 50명의 VU 유지
    { duration: '30s', target: 0 },   // 30초 동안 0명으로 감소
  ],
  thresholds: {
    'http_req_duration': ['p(95)<500'], // 95%의 요청이 500ms 이하
    'http_req_failed': ['rate<0.1'],    // 에러율 10% 미만
    'errors': ['rate<0.1'],              // 커스텀 에러율 10% 미만
  },
};

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080/mobility';

// 테스트 데이터 생성 헬퍼
function generateDispatchData() {
  return {
    startLocation: '서울시 강남구',
    startLatitude: 37.4979 + (Math.random() - 0.5) * 0.1,
    startLongitude: 127.0276 + (Math.random() - 0.5) * 0.1,
    destinationLocation: '서울시 서초구',
    destinationLatitude: 37.4836 + (Math.random() - 0.5) * 0.1,
    destinationLongitude: 127.0327 + (Math.random() - 0.5) * 0.1,
    charge: Math.floor(Math.random() * 50000) + 10000,
    clientPhoneNumber: '010-1234-5678',
    memo: 'K6 부하 테스트',
    call: 'INTERNAL',
    service: 'DELIVERY',
    paymentType: 'CASH',
    tollType: 'TOLLGATE_INCLUDED',
  };
}

export default function () {
  // 1. Office 로그인
  const loginPayload = JSON.stringify({
    loginId: 'test_manager',
    password: 'password123',
  });

  const loginHeaders = {
    'Content-Type': 'application/json',
  };

  const loginRes = http.post(
    `${BASE_URL}/api/v1/auth/office/login`,
    loginPayload,
    { headers: loginHeaders }
  );

  const loginSuccess = check(loginRes, {
    'login status is 200': (r) => r.status === 200,
    'login returns token': (r) => r.json('data.accessToken') !== undefined,
  });

  if (!loginSuccess) {
    errorRate.add(1);
    return;
  }

  const token = loginRes.json('data.accessToken');
  const authHeaders = {
    'Content-Type': 'application/json',
    'Authorization': `Bearer ${token}`,
  };

  // 2. 배차 생성
  const dispatchPayload = JSON.stringify(generateDispatchData());

  const createRes = http.post(
    `${BASE_URL}/api/v1/office/dispatch`,
    dispatchPayload,
    { headers: authHeaders }
  );

  const createSuccess = check(createRes, {
    'create dispatch status is 200': (r) => r.status === 200,
    'dispatch created': (r) => r.json('data.id') !== undefined,
  });

  if (!createSuccess) {
    errorRate.add(1);
    return;
  }

  const dispatchId = createRes.json('data.id');

  sleep(1);

  // 3. 배차 목록 조회
  const listRes = http.get(
    `${BASE_URL}/api/v1/office/dispatch-list?page=0&size=10`,
    { headers: authHeaders }
  );

  check(listRes, {
    'list dispatch status is 200': (r) => r.status === 200,
    'list has content': (r) => r.json('data.content') !== undefined,
  });

  sleep(1);

  // 4. 배차 상세 조회
  const detailRes = http.get(
    `${BASE_URL}/api/v1/office/dispatch/${dispatchId}`,
    { headers: authHeaders }
  );

  check(detailRes, {
    'detail dispatch status is 200': (r) => r.status === 200,
    'detail has id': (r) => r.json('data.id') === dispatchId,
  });

  sleep(1);

  // 5. 배차 수정
  const updatePayload = JSON.stringify({
    charge: 35000,
    memo: 'K6 테스트 - 수정됨',
  });

  const updateRes = http.patch(
    `${BASE_URL}/api/v1/office/dispatch/${dispatchId}`,
    updatePayload,
    { headers: authHeaders }
  );

  check(updateRes, {
    'update dispatch status is 200': (r) => r.status === 200,
  });

  sleep(2);
}

export function handleSummary(data) {
  return {
    'load-test-results.json': JSON.stringify(data, null, 2),
    stdout: textSummary(data, { indent: ' ', enableColors: true }),
  };
}

function textSummary(data, options) {
  return `
========== Load Test Summary ==========
Duration: ${data.metrics.iteration_duration.values.avg.toFixed(2)}ms (avg)
Requests: ${data.metrics.http_reqs.values.count}
Failed: ${data.metrics.http_req_failed.values.rate * 100}%
Request Duration (p95): ${data.metrics.http_req_duration.values['p(95)'].toFixed(2)}ms
Error Rate: ${data.metrics.errors ? data.metrics.errors.values.rate * 100 : 0}%
========================================
  `;
}
