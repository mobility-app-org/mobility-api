import http from 'k6/http';
import { check, sleep } from 'k6';

// Spike Test: 갑작스러운 트래픽 증가에 대한 시스템 반응 테스트
export const options = {
  stages: [
    { duration: '10s', target: 10 },   // 10초 동안 10명으로 증가
    { duration: '1m', target: 10 },    // 1분 동안 10명 유지
    { duration: '10s', target: 100 },  // 10초 동안 급격히 100명으로 증가 (스파이크)
    { duration: '1m', target: 100 },   // 1분 동안 100명 유지
    { duration: '10s', target: 10 },   // 10초 동안 10명으로 감소
    { duration: '1m', target: 10 },    // 1분 동안 10명 유지
    { duration: '10s', target: 0 },    // 10초 동안 0명으로 감소
  ],
  thresholds: {
    'http_req_duration': ['p(95)<1000'], // 95%의 요청이 1초 이하
    'http_req_failed': ['rate<0.2'],     // 에러율 20% 미만 (스파이크 중 일부 실패 허용)
  },
};

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080/mobility';

export default function () {
  const res = http.get(`${BASE_URL}/actuator/health`);

  check(res, {
    'status is 200': (r) => r.status === 200,
  });

  sleep(1);
}
