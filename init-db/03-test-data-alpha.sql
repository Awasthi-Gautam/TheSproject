-- Manual Test Data for Alpha School
-- Run this after docker-compose up to populate test data
-- Usage: docker exec showfolio-test-db psql -U postgres -d showfolio_db -f /docker-entrypoint-initdb.d/03-test-data-alpha.sql

-- First, create the tenant schema using TenantProvisioningService or manually
-- This script assumes sch_test_alpha already exists

SET search_path TO public;

-- 1. Create Principal for Alpha School
INSERT INTO uacn_registry (uacn, aadhaar_hash, name) 
VALUES ('UACN_PRINCIPAL_ALPHA', 'HASH_PRINCIPAL_ALPHA', 'Principal Alpha')
ON CONFLICT (uacn) DO NOTHING;

-- 2. Create Organization
INSERT INTO organizations (id, name, schema_name)
VALUES ('11111111-1111-1111-1111-111111111111'::uuid, 'Alpha School', 'sch_test_alpha')
ON CONFLICT (schema_name) DO NOTHING;

-- 3. Create Org Membership for Principal
INSERT INTO org_memberships (id, member_uacn, org_id, role, joined_at)
VALUES (
    gen_random_uuid(),
    'UACN_PRINCIPAL_ALPHA',
    '11111111-1111-1111-1111-111111111111'::uuid,
    'ADMIN',
    CURRENT_DATE
)
ON CONFLICT DO NOTHING;

-- 4. Create Teacher UACNs
INSERT INTO uacn_registry (uacn, aadhaar_hash, name) VALUES
('UACN_TEACHER_ALPHA_1', 'HASH_TEACHER_ALPHA_1', 'Teacher Alpha 1'),
('UACN_TEACHER_ALPHA_2', 'HASH_TEACHER_ALPHA_2', 'Teacher Alpha 2')
ON CONFLICT (uacn) DO NOTHING;

-- 5. Create Student UACNs (10 students)
INSERT INTO uacn_registry (uacn, aadhaar_hash, name) VALUES
('UACN_STUDENT_ALPHA_001', 'HASH_STUDENT_ALPHA_001', 'Student Alpha 001'),
('UACN_STUDENT_ALPHA_002', 'HASH_STUDENT_ALPHA_002', 'Student Alpha 002'),
('UACN_STUDENT_ALPHA_003', 'HASH_STUDENT_ALPHA_003', 'Student Alpha 003'),
('UACN_STUDENT_ALPHA_004', 'HASH_STUDENT_ALPHA_004', 'Student Alpha 004'),
('UACN_STUDENT_ALPHA_005', 'HASH_STUDENT_ALPHA_005', 'Student Alpha 005'),
('UACN_STUDENT_ALPHA_006', 'HASH_STUDENT_ALPHA_006', 'Student Alpha 006'),
('UACN_STUDENT_ALPHA_007', 'HASH_STUDENT_ALPHA_007', 'Student Alpha 007'),
('UACN_STUDENT_ALPHA_008', 'HASH_STUDENT_ALPHA_008', 'Student Alpha 008'),
('UACN_STUDENT_ALPHA_009', 'HASH_STUDENT_ALPHA_009', 'Student Alpha 009'),
('UACN_STUDENT_ALPHA_010', 'HASH_STUDENT_ALPHA_010', 'Student Alpha 010')
ON CONFLICT (uacn) DO NOTHING;

-- Now switch to tenant schema
SET search_path TO sch_test_alpha;

-- 6. Create Teachers FIRST (required for class foreign keys)
INSERT INTO teachers (uacn, staff_id, department) VALUES
('UACN_TEACHER_ALPHA_1', 'STAFF_A1', 'Mathematics'),
('UACN_TEACHER_ALPHA_2', 'STAFF_A2', 'Science')
ON CONFLICT (uacn) DO NOTHING;

-- 7. Create Subjects
INSERT INTO subjects (id, name) VALUES
('bbbbbbbb-1111-1111-1111-111111111111'::uuid, 'Mathematics'),
('bbbbbbbb-2222-2222-2222-222222222222'::uuid, 'Science'),
('bbbbbbbb-3333-3333-3333-333333333333'::uuid, 'English')
ON CONFLICT (id) DO NOTHING;

-- 8. Create Classes (now that teachers exist)
INSERT INTO classes (id, name, section, class_teacher_uacn) VALUES
('aaaaaaaa-1111-1111-1111-111111111111'::uuid, 'Class 10', 'A', 'UACN_TEACHER_ALPHA_1'),
('aaaaaaaa-2222-2222-2222-222222222222'::uuid, 'Class 10', 'B', 'UACN_TEACHER_ALPHA_2')
ON CONFLICT (id) DO NOTHING;

-- 9. Create Students (5 in Class 10A, 5 in Class 10B)
INSERT INTO students (uacn, roll_number, admission_date, class_id) VALUES
('UACN_STUDENT_ALPHA_001', 'ROLL_001', '2020-04-01', 'aaaaaaaa-1111-1111-1111-111111111111'::uuid),
('UACN_STUDENT_ALPHA_002', 'ROLL_002', '2020-04-01', 'aaaaaaaa-1111-1111-1111-111111111111'::uuid),
('UACN_STUDENT_ALPHA_003', 'ROLL_003', '2020-04-01', 'aaaaaaaa-1111-1111-1111-111111111111'::uuid),
('UACN_STUDENT_ALPHA_004', 'ROLL_004', '2020-04-01', 'aaaaaaaa-1111-1111-1111-111111111111'::uuid),
('UACN_STUDENT_ALPHA_005', 'ROLL_005', '2020-04-01', 'aaaaaaaa-1111-1111-1111-111111111111'::uuid),
('UACN_STUDENT_ALPHA_006', 'ROLL_006', '2020-04-01', 'aaaaaaaa-2222-2222-2222-222222222222'::uuid),
('UACN_STUDENT_ALPHA_007', 'ROLL_007', '2020-04-01', 'aaaaaaaa-2222-2222-2222-222222222222'::uuid),
('UACN_STUDENT_ALPHA_008', 'ROLL_008', '2020-04-01', 'aaaaaaaa-2222-2222-2222-222222222222'::uuid),
('UACN_STUDENT_ALPHA_009', 'ROLL_009', '2020-04-01', 'aaaaaaaa-2222-2222-2222-222222222222'::uuid),
('UACN_STUDENT_ALPHA_010', 'ROLL_010', '2020-04-01', 'aaaaaaaa-2222-2222-2222-222222222222'::uuid)
ON CONFLICT (uacn) DO NOTHING;

-- 10. Create sample Marks
INSERT INTO marks (id, uacn, subject_id, score_obtained, max_score, term) VALUES
(gen_random_uuid(), 'UACN_STUDENT_ALPHA_001', 'bbbbbbbb-1111-1111-1111-111111111111'::uuid, 85, 100, 'Term 1'),
(gen_random_uuid(), 'UACN_STUDENT_ALPHA_001', 'bbbbbbbb-2222-2222-2222-222222222222'::uuid, 90, 100, 'Term 1'),
(gen_random_uuid(), 'UACN_STUDENT_ALPHA_002', 'bbbbbbbb-1111-1111-1111-111111111111'::uuid, 78, 100, 'Term 1'),
(gen_random_uuid(), 'UACN_STUDENT_ALPHA_002', 'bbbbbbbb-2222-2222-2222-222222222222'::uuid, 82, 100, 'Term 1')
ON CONFLICT DO NOTHING;

RESET search_path;
