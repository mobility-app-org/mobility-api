# Database Migrations

이 디렉토리는 수동으로 실행할 데이터베이스 마이그레이션 스크립트를 포함합니다.

## 📁 파일 목록

| 파일 | 설명 | 실행 순서 |
|------|------|----------|
| `20260215_pre_migration_check.sql` | 마이그레이션 전 데이터 상태 확인 (읽기 전용) | 1️⃣ 필수 |
| `20260215_fix_duplicate_assigned_dispatches.sql` | 중복 배차 정리 + UNIQUE INDEX 생성 | 2️⃣ 필수 |
| `20260215_rollback_duplicate_fix.sql` | 마이그레이션 롤백 스크립트 | 3️⃣ 선택 (문제 발생 시) |

---

## 🚀 실행 가이드

### Step 1: 사전 체크 (필수)

마이그레이션 실행 전에 **반드시** 사전 체크를 실행하세요.

```bash
# Docker 환경
docker exec -i mobility-api-postgres psql -U user -d mobilitydb < db-migrations/20260215_pre_migration_check.sql

# 로컬 PostgreSQL
psql -U user -d mobilitydb -f db-migrations/20260215_pre_migration_check.sql
```

**확인 사항**:
- 중복 배차를 가진 기사 수
- 취소될 배차 수 (excess)
- 기사 상태 불일치 여부

### Step 2: 마이그레이션 실행

사전 체크 결과를 확인한 후 마이그레이션을 실행하세요.

```bash
# Docker 환경
docker exec -i mobility-api-postgres psql -U user -d mobilitydb < db-migrations/20260215_fix_duplicate_assigned_dispatches.sql

# 로컬 PostgreSQL
psql -U user -d mobilitydb -f db-migrations/20260215_fix_duplicate_assigned_dispatches.sql
```

**실행 내용**:
1. 백업 테이블 생성 (`dispatch_backup_20260215`)
2. 중복 배차 정리 (최신 배차만 유지)
3. 기사 상태 동기화
4. UNIQUE INDEX 생성 (`idx_unique_assigned_transporter`)

### Step 3: 검증

마이그레이션 후 데이터를 검증하세요.

```sql
-- 중복 배차가 남아있는지 확인
SELECT transporter_id, COUNT(*)
FROM dispatch
WHERE status = 'ASSIGNED' AND active = true AND transporter_id IS NOT NULL
GROUP BY transporter_id
HAVING COUNT(*) > 1;
-- 결과: 0건이어야 함

-- UNIQUE INDEX 생성 확인
SELECT indexname, indexdef
FROM pg_indexes
WHERE indexname = 'idx_unique_assigned_transporter';

-- 기사 상태 일관성 확인
SELECT t.dispatch_status, COUNT(*)
FROM transporters t
LEFT JOIN dispatch d ON t.transporter_id = d.transporter_id
    AND d.status = 'ASSIGNED' AND d.active = true
GROUP BY t.dispatch_status, (d.dispatch_id IS NOT NULL);
```

### Step 4: 애플리케이션 배포

코드 변경사항을 배포하세요.

```bash
# 빌드
./gradlew clean build

# 재시작 (Docker Compose)
docker-compose down
docker-compose up -d

# 로그 확인
docker-compose logs -f api
```

---

## 🔙 롤백 (문제 발생 시)

마이그레이션 후 문제가 발생한 경우에만 실행하세요.

```bash
# Docker 환경
docker exec -i mobility-api-postgres psql -U user -d mobilitydb < db-migrations/20260215_rollback_duplicate_fix.sql

# 로컬 PostgreSQL
psql -U user -d mobilitydb -f db-migrations/20260215_rollback_duplicate_fix.sql
```

**⚠️ 주의**: 롤백 시 중복 데이터가 다시 복원됩니다.

---

## 📊 마이그레이션 상세

### 중복 배차 정리 전략

**기본 전략**: 가장 최근 배차만 유지, 나머지 취소

```sql
-- 기사별로 assigned_at 기준 내림차순 정렬
-- ROW_NUMBER = 1: 유지 (가장 최근)
-- ROW_NUMBER > 1: 취소
```

**대안 전략**:

스크립트를 수정하여 다른 전략을 사용할 수 있습니다:

```sql
-- 옵션 A: 가장 오래된 배차 유지
ORDER BY assigned_at ASC, id ASC

-- 옵션 B: 가장 큰 요금 유지
ORDER BY charge DESC, assigned_at DESC

-- 옵션 C: 특정 배차 ID 수동 지정
WHERE id IN (100, 200, 300)  -- 취소할 배차 ID
```

### UNIQUE INDEX 상세

```sql
CREATE UNIQUE INDEX idx_unique_assigned_transporter
ON dispatch (transporter_id)
WHERE status = 'ASSIGNED' AND active = true;
```

**특징**:
- **부분 인덱스 (Partial Index)**: `status = 'ASSIGNED' AND active = true` 조건만 적용
- **저장 공간 효율**: 전체 배차가 아닌 ASSIGNED 상태만 인덱싱
- **성능**: 해당 조건 검색 시 빠른 조회

**효과**:
- 한 기사는 ASSIGNED 상태 배차를 **하나만** 가질 수 있음
- 중복 INSERT 시도 시 PostgreSQL 에러 발생
- 애플리케이션에서 적절히 에러 처리 필요

---

## 🧪 테스트 시나리오

### 1. 중복 배차 방지 테스트

```sql
-- 기사 1번에게 배차 할당
UPDATE dispatch SET transporter_id = 1, status = 'ASSIGNED' WHERE dispatch_id = 100;

-- 같은 기사에게 또 다른 배차 할당 시도 (실패해야 함)
UPDATE dispatch SET transporter_id = 1, status = 'ASSIGNED' WHERE dispatch_id = 200;
-- ERROR: duplicate key value violates unique constraint "idx_unique_assigned_transporter"
```

### 2. 정상적인 배차 흐름 테스트

```sql
-- 1. 기사 1번에게 배차 할당
UPDATE dispatch SET transporter_id = 1, status = 'ASSIGNED' WHERE dispatch_id = 100;

-- 2. 배차 완료
UPDATE dispatch SET status = 'COMPLETED' WHERE dispatch_id = 100;

-- 3. 새로운 배차 할당 (성공해야 함)
UPDATE dispatch SET transporter_id = 1, status = 'ASSIGNED' WHERE dispatch_id = 200;
```

---

## 📋 체크리스트

마이그레이션 실행 전:
- [ ] 프로덕션 데이터베이스 백업 완료
- [ ] `20260215_pre_migration_check.sql` 실행 완료
- [ ] 중복 배차 데이터 검토 완료
- [ ] 영업팀 및 기사에게 점검 시간 공지
- [ ] 애플리케이션 코드 변경사항 준비 완료

마이그레이션 실행 후:
- [ ] 중복 데이터 제거 확인 (0건)
- [ ] UNIQUE INDEX 생성 확인
- [ ] 기사 상태 일관성 확인
- [ ] 애플리케이션 재시작
- [ ] API 동작 테스트 (`/api/v1/transporter/current-dispatch`)
- [ ] 새로운 배차 할당 테스트
- [ ] 로그에서 `TRANSPORTER_ALREADY_DISPATCHED` 에러 모니터링

1주일 후:
- [ ] 백업 테이블 삭제 (`DROP TABLE dispatch_backup_20260215;`)

---

## 🛠 트러블슈팅

### Q1. "duplicate key value" 에러 발생 시

**원인**: UNIQUE INDEX 생성 전에 중복 데이터가 제거되지 않음

**해결**:
```sql
-- Step 3를 다시 실행하여 중복 제거
-- 그 후 Step 6의 INDEX 생성 재시도
```

### Q2. 마이그레이션 실행 중 "Migration validation failed" 에러

**원인**: 중복 데이터가 완전히 제거되지 않음

**해결**:
```sql
-- 수동으로 중복 확인
SELECT transporter_id, COUNT(*)
FROM dispatch
WHERE status = 'ASSIGNED' AND active = true AND transporter_id IS NOT NULL
GROUP BY transporter_id
HAVING COUNT(*) > 1;

-- 해당 레코드 수동 처리
```

### Q3. 롤백 후 애플리케이션 에러 계속 발생

**원인**: 코드는 이미 변경되었으나 DB는 롤백됨

**해결**:
1. 코드를 이전 버전으로 롤백
2. 또는 마이그레이션을 다시 실행

### Q4. 백업 테이블이 너무 큰 경우

**확인**:
```sql
SELECT pg_size_pretty(pg_total_relation_size('dispatch_backup_20260215'));
```

**삭제**:
```sql
-- 1주일 후 안전하게 삭제
DROP TABLE IF EXISTS dispatch_backup_20260215;
```

---

## 📞 지원

문제 발생 시:
1. 로그 확인: `/var/log/postgresql/`, `docker-compose logs -f postgres`
2. 백업 테이블 확인: `SELECT * FROM dispatch_backup_20260215 LIMIT 10;`
3. GitHub Issue 생성: [프로젝트 이슈 링크]

---

## 📚 관련 문서

- [CONCURRENT_DISPATCH_ISSUE.md](../CONCURRENT_DISPATCH_ISSUE.md) - 문제 분석 및 해결 방안
- [CLAUDE.md](../CLAUDE.md) - 프로젝트 아키텍처 및 데이터베이스 구조
