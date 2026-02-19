-- ================================================================
-- Migration: Fix Duplicate ASSIGNED Dispatches
-- Date: 2026-02-15
-- Description:
--   - 한 기사가 여러 개의 ASSIGNED 배차를 가지는 문제 해결
--   - 중복 배차 정리 (가장 최근 배차만 유지)
--   - 기사 상태(dispatch_status) 동기화
--   - UNIQUE INDEX 생성으로 중복 방지
-- ================================================================

BEGIN;

-- ================================================================
-- Step 1: 백업 테이블 생성 (롤백 및 추적용)
-- ================================================================
CREATE TABLE IF NOT EXISTS dispatch_backup_20260215 AS
SELECT * FROM dispatch
WHERE status = 'ASSIGNED' AND active = true;

-- 백업된 레코드 수 출력
DO $$
DECLARE
    backup_count INTEGER;
BEGIN
    SELECT COUNT(*) INTO backup_count FROM dispatch_backup_20260215;
    RAISE NOTICE 'Created backup table with % records', backup_count;
END $$;

-- ================================================================
-- Step 2: 중복 데이터 확인 및 로그
-- ================================================================
DO $$
DECLARE
    duplicate_transporter_count INTEGER;
    total_duplicate_dispatch_count INTEGER;
BEGIN
    -- 중복 배차를 가진 기사 수
    SELECT COUNT(*) INTO duplicate_transporter_count
    FROM (
        SELECT transporter_id
        FROM dispatch
        WHERE status = 'ASSIGNED'
          AND active = true
          AND transporter_id IS NOT NULL
        GROUP BY transporter_id
        HAVING COUNT(*) > 1
    ) duplicates;

    -- 중복 배차 총 개수
    SELECT SUM(cnt - 1) INTO total_duplicate_dispatch_count
    FROM (
        SELECT COUNT(*) as cnt
        FROM dispatch
        WHERE status = 'ASSIGNED'
          AND active = true
          AND transporter_id IS NOT NULL
        GROUP BY transporter_id
        HAVING COUNT(*) > 1
    ) dup_counts;

    IF duplicate_transporter_count > 0 THEN
        RAISE NOTICE '========================================';
        RAISE NOTICE 'Found % transporters with duplicate ASSIGNED dispatches', duplicate_transporter_count;
        RAISE NOTICE 'Total % dispatches will be canceled', total_duplicate_dispatch_count;
        RAISE NOTICE '========================================';
    ELSE
        RAISE NOTICE 'No duplicate ASSIGNED dispatches found';
    END IF;
END $$;

-- ================================================================
-- Step 3: 중복 배차 정리 (가장 최근 배차만 유지, 나머지 취소)
-- ================================================================
WITH ranked_dispatches AS (
    SELECT
        id,
        transporter_id,
        assigned_at,
        ROW_NUMBER() OVER (
            PARTITION BY transporter_id
            ORDER BY assigned_at DESC, id DESC  -- 최신 배차 우선
        ) as rn
    FROM dispatch
    WHERE status = 'ASSIGNED'
      AND active = true
      AND transporter_id IS NOT NULL
)
UPDATE dispatch
SET
    status = 'CANCELED',
    canceled_at = NOW(),
    updated_at = NOW()
FROM ranked_dispatches
WHERE dispatch.id = ranked_dispatches.id
  AND ranked_dispatches.rn > 1;

-- 취소된 레코드 수 출력
DO $$
DECLARE
    canceled_count INTEGER;
BEGIN
    -- 마지막 UPDATE로 영향받은 행 수는 별도 조회 필요
    SELECT COUNT(*) INTO canceled_count
    FROM dispatch
    WHERE status = 'CANCELED'
      AND canceled_at >= NOW() - INTERVAL '5 seconds';  -- 방금 취소된 것만

    RAISE NOTICE 'Canceled % duplicate dispatches', canceled_count;
END $$;

-- ================================================================
-- Step 4: 기사 상태(dispatch_status) 동기화
-- ================================================================

-- 4-1. ASSIGNED 배차가 없는데 DISPATCH 상태인 기사 찾기
DO $$
DECLARE
    inconsistent_count INTEGER;
BEGIN
    SELECT COUNT(*) INTO inconsistent_count
    FROM transporters t
    LEFT JOIN dispatch d ON t.transporter_id = d.transporter_id
        AND d.status = 'ASSIGNED'
        AND d.active = true
    WHERE t.dispatch_status = 'DISPATCH'
      AND d.dispatch_id IS NULL;

    IF inconsistent_count > 0 THEN
        RAISE NOTICE 'Found % transporters with inconsistent dispatch_status', inconsistent_count;
    END IF;
END $$;

-- 4-2. 기사 상태를 EMPTY로 변경
UPDATE transporters t
SET dispatch_status = 'EMPTY'
WHERE t.dispatch_status = 'DISPATCH'
  AND NOT EXISTS (
      SELECT 1 FROM dispatch d
      WHERE d.transporter_id = t.transporter_id
        AND d.status = 'ASSIGNED'
        AND d.active = true
  );

-- ================================================================
-- Step 5: 검증 - 중복 데이터가 남아있는지 확인
-- ================================================================
DO $$
DECLARE
    duplicate_count INTEGER;
BEGIN
    SELECT COUNT(*) INTO duplicate_count
    FROM (
        SELECT transporter_id
        FROM dispatch
        WHERE status = 'ASSIGNED'
          AND active = true
          AND transporter_id IS NOT NULL
        GROUP BY transporter_id
        HAVING COUNT(*) > 1
    ) duplicates;

    IF duplicate_count > 0 THEN
        RAISE EXCEPTION 'Migration validation failed: Still have % transporters with duplicate assigned dispatches', duplicate_count;
    ELSE
        RAISE NOTICE '✓ Validation passed: No duplicate assigned dispatches found';
    END IF;
END $$;

-- ================================================================
-- Step 6: UNIQUE INDEX 생성
-- ================================================================

-- 기존 인덱스가 있다면 삭제
DROP INDEX IF EXISTS idx_unique_assigned_transporter;

-- 부분 UNIQUE INDEX 생성
-- (한 기사는 ASSIGNED 상태 배차를 하나만 가질 수 있음)
CREATE UNIQUE INDEX idx_unique_assigned_transporter
ON dispatch (transporter_id)
WHERE status = 'ASSIGNED' AND active = true;

RAISE NOTICE '✓ Successfully created UNIQUE INDEX: idx_unique_assigned_transporter';

-- ================================================================
-- Step 7: 인덱스 정보 출력
-- ================================================================
DO $$
DECLARE
    index_def TEXT;
BEGIN
    SELECT indexdef INTO index_def
    FROM pg_indexes
    WHERE indexname = 'idx_unique_assigned_transporter';

    RAISE NOTICE '========================================';
    RAISE NOTICE 'Index definition:';
    RAISE NOTICE '%', index_def;
    RAISE NOTICE '========================================';
END $$;

COMMIT;

-- ================================================================
-- 마이그레이션 완료 메시지
-- ================================================================
DO $$
BEGIN
    RAISE NOTICE '========================================';
    RAISE NOTICE '✓ Migration completed successfully';
    RAISE NOTICE '========================================';
    RAISE NOTICE 'Next steps:';
    RAISE NOTICE '1. Verify data integrity';
    RAISE NOTICE '2. Monitor application logs for TRANSPORTER_ALREADY_DISPATCHED errors';
    RAISE NOTICE '3. After 1 week, drop backup table: DROP TABLE dispatch_backup_20260215;';
    RAISE NOTICE '========================================';
END $$;
