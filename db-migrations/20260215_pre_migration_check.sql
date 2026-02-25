-- ================================================================
-- Pre-Migration Check: Duplicate ASSIGNED Dispatches
-- Date: 2026-02-15
-- Description:
--   마이그레이션 실행 전에 현재 데이터 상태를 확인합니다.
--   이 스크립트는 데이터를 변경하지 않으며, 읽기 전용입니다.
-- ================================================================

-- ================================================================
-- Check 1: 중복 배차를 가진 기사 목록
-- ================================================================
SELECT
    '=== Transporters with Duplicate ASSIGNED Dispatches ===' as check_title;

SELECT
    t.transporter_id,
    t.name as transporter_name,
    t.phone,
    t.dispatch_status,
    COUNT(d.id) as assigned_count,
    STRING_AGG(d.id::text, ', ' ORDER BY d.assigned_at DESC) as dispatch_ids,
    STRING_AGG(d.assigned_at::text, ', ' ORDER BY d.assigned_at DESC) as assigned_times,
    STRING_AGG(d.charge::text, ', ' ORDER BY d.assigned_at DESC) as charges
FROM dispatch d
JOIN transporters t ON d.transporter_id = t.transporter_id
WHERE d.status = 'ASSIGNED'
  AND d.active = true
  AND d.transporter_id IS NOT NULL
GROUP BY t.transporter_id, t.name, t.phone, t.dispatch_status
HAVING COUNT(d.id) > 1
ORDER BY COUNT(d.id) DESC, t.transporter_id;

-- ================================================================
-- Check 2: 중복 배차 통계
-- ================================================================
SELECT
    '=== Duplicate Dispatch Statistics ===' as check_title;

SELECT
    COUNT(DISTINCT transporter_id) as transporters_with_duplicates,
    SUM(duplicate_count - 1) as total_excess_dispatches,
    MAX(duplicate_count) as max_duplicates_per_transporter,
    ROUND(AVG(duplicate_count), 2) as avg_duplicates_per_transporter
FROM (
    SELECT
        transporter_id,
        COUNT(*) as duplicate_count
    FROM dispatch
    WHERE status = 'ASSIGNED'
      AND active = true
      AND transporter_id IS NOT NULL
    GROUP BY transporter_id
    HAVING COUNT(*) > 1
) dup_stats;

-- ================================================================
-- Check 3: 기사 상태 불일치 확인
-- ================================================================
SELECT
    '=== Inconsistent Transporter Dispatch Status ===' as check_title;

-- Case 1: ASSIGNED 배차가 있는데 EMPTY 상태인 기사
SELECT
    '1. Has ASSIGNED dispatch but status is EMPTY' as inconsistency_type,
    t.transporter_id,
    t.name,

    t.dispatch_status,
    COUNT(d.id) as assigned_dispatch_count
FROM transporters t
JOIN dispatch d ON t.transporter_id = d.transporter_id
WHERE d.status = 'ASSIGNED'
  AND d.active = true
  AND t.dispatch_status = 'EMPTY'
GROUP BY t.transporter_id, t.name, t.dispatch_status

UNION ALL

-- Case 2: ASSIGNED 배차가 없는데 DISPATCH 상태인 기사
SELECT
    '2. No ASSIGNED dispatch but status is DISPATCH' as inconsistency_type,
    t.transporter_id,
    t.name,
    t.dispatch_status,
    0 as assigned_dispatch_count
FROM transporters t
LEFT JOIN dispatch d ON t.transporter_id = d.transporter_id
    AND d.status = 'ASSIGNED'
    AND d.active = true
WHERE t.dispatch_status = 'DISPATCH'
  AND d.id IS NULL;

-- ================================================================
-- Check 4: 중복 배차 상세 정보 (샘플 5건)
-- ================================================================
SELECT
    '=== Sample Duplicate Dispatches (Top 5) ===' as check_title;

SELECT
    d.id,
    d.transporter_id,
    t.name as transporter_name,
    d.status,
    d.charge,
    d.start_location,
    d.destination_location,
    d.assigned_at,
    d.created_at,
    ROW_NUMBER() OVER (PARTITION BY d.transporter_id ORDER BY d.assigned_at DESC) as rank_in_group
FROM dispatch d
JOIN transporters t ON d.transporter_id = t.transporter_id
WHERE d.transporter_id IN (
    SELECT transporter_id
    FROM dispatch
    WHERE status = 'ASSIGNED' AND active = true
    GROUP BY transporter_id
    HAVING COUNT(*) > 1
    LIMIT 5
)
AND d.status = 'ASSIGNED'
AND d.active = true
ORDER BY d.transporter_id, d.assigned_at DESC;

-- ================================================================
-- Check 5: 현재 UNIQUE INDEX 존재 여부
-- ================================================================
SELECT
    '=== Current Index Status ===' as check_title;

SELECT
    indexname,
    indexdef
FROM pg_indexes
WHERE indexname = 'idx_unique_assigned_transporter';

-- 인덱스가 없으면 메시지 출력
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_indexes WHERE indexname = 'idx_unique_assigned_transporter') THEN
        RAISE NOTICE 'INDEX idx_unique_assigned_transporter does not exist';
    ELSE
        RAISE NOTICE 'INDEX idx_unique_assigned_transporter already exists';
    END IF;
END $$;

-- ================================================================
-- Check 6: 마이그레이션 영향 예측
-- ================================================================
SELECT
    '=== Migration Impact Estimate ===' as check_title;

SELECT
    'Total ASSIGNED dispatches' as metric,
    COUNT(*) as count
FROM dispatch
WHERE status = 'ASSIGNED' AND active = true

UNION ALL

SELECT
    'Dispatches to be canceled (excess)' as metric,
    COALESCE(SUM(cnt - 1), 0) as count
FROM (
    SELECT COUNT(*) as cnt
    FROM dispatch
    WHERE status = 'ASSIGNED' AND active = true AND transporter_id IS NOT NULL
    GROUP BY transporter_id
    HAVING COUNT(*) > 1
) dup_counts

UNION ALL

SELECT
    'Dispatches to remain ASSIGNED' as metric,
    COUNT(*) - COALESCE((SELECT SUM(cnt - 1) FROM (
        SELECT COUNT(*) as cnt
        FROM dispatch
        WHERE status = 'ASSIGNED' AND active = true AND transporter_id IS NOT NULL
        GROUP BY transporter_id
        HAVING COUNT(*) > 1
    ) dup_counts), 0) as count
FROM dispatch
WHERE status = 'ASSIGNED' AND active = true;

-- ================================================================
-- 최종 요약
-- ================================================================
DO $$
DECLARE
    dup_count INTEGER;
BEGIN
    SELECT COUNT(*) INTO dup_count
    FROM (
        SELECT transporter_id
        FROM dispatch
        WHERE status = 'ASSIGNED' AND active = true AND transporter_id IS NOT NULL
        GROUP BY transporter_id
        HAVING COUNT(*) > 1
    ) duplicates;

    RAISE NOTICE '========================================';
    RAISE NOTICE 'Pre-Migration Check Summary';
    RAISE NOTICE '========================================';

    IF dup_count > 0 THEN
        RAISE NOTICE '⚠️  Found % transporters with duplicate ASSIGNED dispatches', dup_count;
        RAISE NOTICE '✓ Migration is REQUIRED';
        RAISE NOTICE '';
        RAISE NOTICE 'Review the results above before proceeding with migration.';
        RAISE NOTICE 'Execute: 20260215_fix_duplicate_assigned_dispatches.sql';
    ELSE
        RAISE NOTICE '✓ No duplicate ASSIGNED dispatches found';
        RAISE NOTICE '✓ Migration is NOT REQUIRED';
        RAISE NOTICE '';
        RAISE NOTICE 'You can still create the UNIQUE INDEX to prevent future duplicates.';
    END IF;

    RAISE NOTICE '========================================';
END $$;
