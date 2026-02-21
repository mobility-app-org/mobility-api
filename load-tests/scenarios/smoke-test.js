import http from 'k6/http';
import { check, sleep } from 'k6';

// Smoke Test: 최소한의 부하로 시스템이 정상 작동하는지 확인
export const options = {
  vus: 1, // 1 virtual user
  duration: '1m', // 1분 동안
  thresholds: {
    'http_req_duration': ['p(99)<1000'], // 99%의 요청이 1초 이하
    'http_req_failed': ['rate<0.01'],    // 에러율 1% 미만
  },
};

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080/mobility';

export default function () {
  // Health Check
  const healthRes = http.get(`${BASE_URL}/actuator/health`);
  check(healthRes, {
    'health check status is 200': (r) => r.status === 200,
    'health check is UP': (r) => r.json('status') === 'UP',
  });

  sleep(1);

  // Prometheus Metrics
  const metricsRes = http.get(`${BASE_URL}/actuator/prometheus`);
  check(metricsRes, {
    'metrics status is 200': (r) => r.status === 200,
    'metrics has content': (r) => r.body.length > 0,
  });

  sleep(1);

  // Info Endpoint
  const infoRes = http.get(`${BASE_URL}/actuator/info`);
  check(infoRes, {
    'info status is 200': (r) => r.status === 200,
  });

  sleep(2);
}
