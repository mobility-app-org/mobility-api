-- ================================================================
-- Rollback Script: Revert Duplicate ASSIGNED Dispatches Fix
-- Date: 2026-02-15
-- Description:
--   마이그레이션을 롤백하고 백업에서 데이터를 복원합니다.
--   ⚠️ 주의: 이 스크립트를 실행하면 중복 데이터가 다시 복원됩니다.
-- ================================================================

BEGIN;

-- ================================================================
-- Step 1: 백업 테이블 존재 확인
-- ================================================================
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'dispatch_backup_20260215') THEN
        RAISE EXCEPTION 'Backup table dispatch_backup_20260215 does not exist. Cannot rollback.';
    END IF;
    RAISE NOTICE 'Backup table found. Proceeding with rollback...';
END $$;

-- ================================================================
-- Step 2: UNIQUE INDEX 삭제
-- ================================================================
DROP INDEX IF EXISTS idx_unique_assigned_transporter;
RAISE NOTICE '✓ Dropped UNIQUE INDEX: idx_unique_assigned_transporter';

-- ================================================================
-- Step 3: 백업에서 데이터 복원
-- ================================================================

-- 3-1. 마이그레이션으로 취소된 배차 복원
UPDATE dispatch
SET
    status = backup.status,
    canceled_at = backup.canceled_at,
    updated_at = backup.updated_at
FROM dispatch_backup_20260215 backup
WHERE dispatch.id = backup.id
  AND dispatch.status = 'CANCELED'
  AND backup.status = 'ASSIGNED';

-- 복원된 레코드 수 출력
DO $$
DECLARE
    restored_count INTEGER;
BEGIN
    SELECT COUNT(*) INTO restored_count
    FROM dispatch d
    JOIN dispatch_backup_20260215 b ON d.id = b.id
    WHERE d.status = 'ASSIGNED' AND b.status = 'ASSIGNED';

    RAISE NOTICE '✓ Restored % dispatches from backup', restored_count;
END $$;

-- ================================================================
-- Step 4: 기사 상태 복원 (필요한 경우)
-- ================================================================

-- ASSIGNED 배차가 있는데 EMPTY 상태인 기사를 DISPATCH로 변경
UPDATE transporters t
SET dispatch_status = 'DISPATCH'
WHERE t.dispatch_status = 'EMPTY'
  AND EXISTS (
      SELECT 1 FROM dispatch d
      WHERE d.transporter_id = t.transporter_id
        AND d.status = 'ASSIGNED'
        AND d.active = true
  );

-- ================================================================
-- Step 5: 검증
-- ================================================================
DO $$
DECLARE
    current_assigned_count INTEGER;
    backup_assigned_count INTEGER;
BEGIN
    SELECT COUNT(*) INTO current_assigned_count
    FROM dispatch
    WHERE status = 'ASSIGNED' AND active = true;

    SELECT COUNT(*) INTO backup_assigned_count
    FROM dispatch_backup_20260215;

    RAISE NOTICE '========================================';
    RAISE NOTICE 'Rollback validation:';
    RAISE NOTICE 'Current ASSIGNED dispatches: %', current_assigned_count;
    RAISE NOTICE 'Backup ASSIGNED dispatches: %', backup_assigned_count;

    IF current_assigned_count = backup_assigned_count THEN
        RAISE NOTICE '✓ Data restored successfully';
    ELSE
        RAISE WARNING 'Data count mismatch. Please verify manually.';
    END IF;
    RAISE NOTICE '========================================';
END $$;

COMMIT;

-- ================================================================
-- 롤백 완료 메시지
-- ================================================================
DO $$
BEGIN
    RAISE NOTICE '========================================';
    RAISE NOTICE '✓ Rollback completed';
    RAISE NOTICE '⚠️  WARNING: Duplicate ASSIGNED dispatches are now restored';
    RAISE NOTICE '========================================';
    RAISE NOTICE 'Next steps:';
    RAISE NOTICE '1. Verify application behavior';
    RAISE NOTICE '2. Investigate root cause of migration issues';
    RAISE NOTICE '3. Plan re-migration strategy';
    RAISE NOTICE '========================================';
END $$;
