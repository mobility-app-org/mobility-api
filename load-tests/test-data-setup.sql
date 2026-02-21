-- 테스트 데이터 생성 스크립트
-- 부하 테스트를 위한 Office, Manager, Transporter 데이터 생성

-- 1. Office 데이터 생성
INSERT INTO office (office_name, office_registration_number, office_address, office_tel_number, created_at, updated_at)
VALUES
    ('테스트 사무소 1', '123-45-67890', '서울시 강남구 테헤란로 123', '02-1234-5678', NOW(), NOW()),
    ('테스트 사무소 2', '123-45-67891', '서울시 서초구 반포대로 456', '02-2345-6789', NOW(), NOW()),
    ('테스트 사무소 3', '123-45-67892', '서울시 송파구 올림픽로 789', '02-3456-7890', NOW(), NOW())
ON CONFLICT DO NOTHING;

-- 2. Manager 데이터 생성 (테스트용 계정)
-- 비밀번호: password123 (BCrypt 해시)
INSERT INTO manager (login_id, password, name, phone, email, role, office_id, created_at, updated_at)
VALUES
    ('test_manager', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '테스트 관리자', '010-0000-0001', 'test1@test.com', 'OWNER', 1, NOW(), NOW()),
    ('test_manager2', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '테스트 관리자2', '010-0000-0002', 'test2@test.com', 'OWNER', 2, NOW(), NOW()),
    ('test_manager3', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '테스트 관리자3', '010-0000-0003', 'test3@test.com', 'OWNER', 3, NOW(), NOW())
ON CONFLICT DO NOTHING;

-- 3. Transporter 데이터 생성 (자동배차 활성화)
INSERT INTO transporters (name, phone, current_location, is_auto_dispatch, dispatch_status, created_at, updated_at)
VALUES
    ('기사1', '010-1111-0001', ST_SetSRID(ST_MakePoint(127.0276, 37.4979), 4326), true, 'EMPTY', NOW(), NOW()),
    ('기사2', '010-1111-0002', ST_SetSRID(ST_MakePoint(127.0327, 37.4836), 4326), true, 'EMPTY', NOW(), NOW()),
    ('기사3', '010-1111-0003', ST_SetSRID(ST_MakePoint(127.0474, 37.5172), 4326), true, 'EMPTY', NOW(), NOW()),
    ('기사4', '010-1111-0004', ST_SetSRID(ST_MakePoint(127.0630, 37.5142), 4326), true, 'EMPTY', NOW(), NOW()),
    ('기사5', '010-1111-0005', ST_SetSRID(ST_MakePoint(127.0016, 37.5665), 4326), true, 'EMPTY', NOW(), NOW()),
    ('기사6', '010-1111-0006', ST_SetSRID(ST_MakePoint(126.9780, 37.5663), 4326), true, 'EMPTY', NOW(), NOW()),
    ('기사7', '010-1111-0007', ST_SetSRID(ST_MakePoint(126.9996, 37.5512), 4326), true, 'EMPTY', NOW(), NOW()),
    ('기사8', '010-1111-0008', ST_SetSRID(ST_MakePoint(127.0276, 37.4979), 4326), true, 'EMPTY', NOW(), NOW()),
    ('기사9', '010-1111-0009', ST_SetSRID(ST_MakePoint(127.0327, 37.4836), 4326), true, 'EMPTY', NOW(), NOW()),
    ('기사10', '010-1111-0010', ST_SetSRID(ST_MakePoint(127.0474, 37.5172), 4326), true, 'EMPTY', NOW(), NOW())
ON CONFLICT DO NOTHING;

-- 4. Location History 초기 데이터 생성
INSERT INTO location_history (transporter_id, location, created_at)
SELECT
    t.id,
    t.current_location,
    NOW()
FROM transporters t
WHERE t.phone LIKE '010-1111-%'
ON CONFLICT DO NOTHING;

-- 5. 샘플 배차 데이터 생성 (다양한 상태)
INSERT INTO dispatch (
    start_location, start_latitude, start_longitude,
    destination_location, destination_latitude, destination_longitude,
    charge, client_phone_number, memo,
    status, call, service, payment_type, toll_type,
    office_id, active, created_at, updated_at
)
VALUES
    -- OPEN 상태 배차들
    ('서울 강남구 역삼동', 37.5000, 127.0400, '서울 서초구 서초동', 37.4800, 127.0200, 25000, '010-2222-0001', '테스트 배차 1', 'OPEN', 'INTERNAL', 'DELIVERY', 'CASH', 'TOLLGATE_INCLUDED', 1, true, NOW(), NOW()),
    ('서울 송파구 잠실동', 37.5130, 127.1000, '서울 강동구 천호동', 37.5380, 127.1240, 28000, '010-2222-0002', '테스트 배차 2', 'OPEN', 'INTERNAL', 'DELIVERY', 'POSTPAID', 'TOLLGATE_SEPARATE', 1, true, NOW(), NOW()),
    ('서울 강서구 화곡동', 37.5400, 126.8400, '서울 양천구 목동', 37.5260, 126.8750, 22000, '010-2222-0003', '테스트 배차 3', 'OPEN', 'INTEGRATED', 'DRIVER', 'COMPLETE_POSTPAID', 2, true, NOW(), NOW()),
    ('서울 마포구 공덕동', 37.5440, 126.9510, '서울 용산구 이촌동', 37.5220, 126.9650, 20000, '010-2222-0004', '테스트 배차 4', 'OPEN', 'INTERNAL', 'DELIVERY', 'CASH', 'HIPASS', 2, true, NOW(), NOW()),
    ('서울 성북구 정릉동', 37.6100, 127.0050, '서울 강북구 수유동', 37.6390, 127.0250, 18000, '010-2222-0005', '테스트 배차 5', 'OPEN', 'INTERNAL', 'DELIVERY', 'CASH', 'TOLLGATE_INCLUDED', 3, true, NOW(), NOW())
ON CONFLICT DO NOTHING;

-- 성공 메시지
SELECT 'Test data setup completed successfully!' as message;
SELECT COUNT(*) as office_count FROM office;
SELECT COUNT(*) as manager_count FROM manager;
SELECT COUNT(*) as transporter_count FROM transporters;
SELECT COUNT(*) as dispatch_count FROM dispatch;
