-- Create tables in sch_test_alpha schema for manual testing
-- This script creates the same structure as the tenant_template.sql

SET search_path TO sch_test_alpha;

-- Teachers table
CREATE TABLE IF NOT EXISTS teachers (
    uacn VARCHAR(255) PRIMARY KEY,
    staff_id VARCHAR(50) NOT NULL,
    department VARCHAR(100)
);

-- Classes table
CREATE TABLE IF NOT EXISTS classes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(100) NOT NULL,
    section VARCHAR(10),
    class_teacher_uacn VARCHAR(255),
    FOREIGN KEY (class_teacher_uacn) REFERENCES teachers(uacn)
);

-- Subjects table
CREATE TABLE IF NOT EXISTS subjects (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(100) NOT NULL UNIQUE
);

-- Students table  
CREATE TABLE IF NOT EXISTS students (
    uacn VARCHAR(255) PRIMARY KEY,
    roll_number VARCHAR(50) NOT NULL,
    admission_date DATE NOT NULL,
    class_id UUID,
    FOREIGN KEY (class_id) REFERENCES classes(id)
);

-- Marks table
CREATE TABLE IF NOT EXISTS marks (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    uacn VARCHAR(255) NOT NULL,
    subject_id UUID NOT NULL,
    score_obtained NUMERIC(5,2) NOT NULL,
    max_score NUMERIC(5,2) NOT NULL,
    term VARCHAR(50),
    FOREIGN KEY (uacn) REFERENCES students(uacn),
    FOREIGN KEY (subject_id) REFERENCES subjects(id)
);

-- Attendance table
CREATE TABLE IF NOT EXISTS attendance (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    uacn VARCHAR(255) NOT NULL,
    date DATE NOT NULL,
    status VARCHAR(20) NOT NULL,
    FOREIGN KEY (uacn) REFERENCES students(uacn)
);

-- Subject Assignments (Teacher-Subject mapping)
CREATE TABLE IF NOT EXISTS subject_assignments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    teacher_uacn VARCHAR(255) NOT NULL,
    subject_id UUID NOT NULL,
    class_id UUID NOT NULL,
    FOREIGN KEY (teacher_uacn) REFERENCES teachers(uacn),
    FOREIGN KEY (subject_id) REFERENCES subjects(id),
    FOREIGN KEY (class_id) REFERENCES classes(id),
    UNIQUE(teacher_uacn, subject_id, class_id)
);

RESET search_path;
