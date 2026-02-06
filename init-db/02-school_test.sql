-- Create the tenant schema for the legacy demo "school_test"
CREATE SCHEMA IF NOT EXISTS school_test;

-- Create the tenant schema for manual testing "sch_test_alpha"
CREATE SCHEMA IF NOT EXISTS sch_test_alpha;

-- Setup school_test schema
SET search_path TO school_test;

-- Create base tables first (no foreign key dependencies)
CREATE TABLE IF NOT EXISTS teachers (
    uacn VARCHAR(255) PRIMARY KEY,
    staff_id VARCHAR(50),
    department VARCHAR(100)
);

CREATE TABLE IF NOT EXISTS classes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(100) NOT NULL,
    section VARCHAR(10),
    class_teacher_uacn VARCHAR(255)
);

CREATE TABLE IF NOT EXISTS subjects (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(100) NOT NULL
);

-- Now create student table (references classes)
CREATE TABLE IF NOT EXISTS students (
    uacn VARCHAR(255) PRIMARY KEY,
    roll_number VARCHAR(50),
    admission_date DATE,
    class_id UUID,
    FOREIGN KEY (class_id) REFERENCES classes(id)
    -- Note: Foreign key to public.uacn_registry technically works in PG if you qualify it,
    -- but usually JPA handles validation. The DB constraint is good to have.
    -- FOREIGN KEY (uacn) REFERENCES public.uacn_registry(uacn)
);

-- Create tables that reference students, classes, subjects, or teachers
CREATE TABLE IF NOT EXISTS marks (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    uacn VARCHAR(255) NOT NULL,
    subject_id UUID NOT NULL,
    score_obtained INT,
    max_score INT,
    term VARCHAR(50),
    FOREIGN KEY (uacn) REFERENCES students(uacn),
    FOREIGN KEY (subject_id) REFERENCES subjects(id)
);

CREATE TABLE IF NOT EXISTS attendance (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    uacn VARCHAR(255) NOT NULL,
    date DATE NOT NULL,
    status VARCHAR(20),
    FOREIGN KEY (uacn) REFERENCES students(uacn)
);

CREATE TABLE IF NOT EXISTS timetable (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    class_id UUID NOT NULL,
    subject_id UUID NOT NULL,
    teacher_uacn VARCHAR(255) NOT NULL,
    day VARCHAR(20),
    time_slot VARCHAR(50),
    FOREIGN KEY (class_id) REFERENCES classes(id),
    FOREIGN KEY (subject_id) REFERENCES subjects(id),
    FOREIGN KEY (teacher_uacn) REFERENCES teachers(uacn)
);

CREATE TABLE IF NOT EXISTS student_contacts (
    uacn VARCHAR(255) PRIMARY KEY,
    address TEXT,
    phone VARCHAR(20),
    email VARCHAR(100),
    emergency_contact VARCHAR(20),
    FOREIGN KEY (uacn) REFERENCES students(uacn)
);

CREATE TABLE IF NOT EXISTS parent_details (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    uacn VARCHAR(255) NOT NULL,
    father_name VARCHAR(255),
    mother_name VARCHAR(255),
    primary_phone VARCHAR(20),
    occupation VARCHAR(100),
    FOREIGN KEY (uacn) REFERENCES students(uacn)
);

CREATE TABLE IF NOT EXISTS fee_installments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    uacn VARCHAR(255) NOT NULL,
    category_name VARCHAR(100),
    amount_due DECIMAL(10, 2),
    due_date DATE,
    status VARCHAR(20),
    FOREIGN KEY (uacn) REFERENCES students(uacn)
);

CREATE TABLE IF NOT EXISTS fee_payments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    installment_id UUID NOT NULL,
    amount_paid DECIMAL(10, 2),
    payment_date DATE,
    payment_mode VARCHAR(50),
    transaction_ref VARCHAR(100),
    FOREIGN KEY (installment_id) REFERENCES fee_installments(id)
);

CREATE TABLE IF NOT EXISTS subject_assignments (
    id UUID PRIMARY KEY,
    uacn VARCHAR(255),
    subject_id UUID,
    class_id UUID,
    FOREIGN KEY (class_id) REFERENCES classes(id)
    -- FOREIGN KEY (subject_id) REFERENCES subjects(id) -- Assuming subjects table exists
);

CREATE TABLE IF NOT EXISTS audit_logs (
    id UUID PRIMARY KEY,
    action_by_uacn VARCHAR(255),
    target_uacn VARCHAR(255),
    action_type VARCHAR(50),
    timestamp TIMESTAMP
);
