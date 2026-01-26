-- Create the test tenant schema
CREATE SCHEMA IF NOT EXISTS school_test;

SET search_path TO school_test;

-- Execute the template logic (copy-pasted from template for this init script)
CREATE TABLE IF NOT EXISTS students (
    uacn VARCHAR(255) PRIMARY KEY,
    roll_number VARCHAR(50),
    admission_date DATE
    -- Note: Foreign key to public.uacn_registry technically works in PG if you qualify it,
    -- but usually JPA handles validation. The DB constraint is good to have.
    -- FOREIGN KEY (uacn) REFERENCES public.uacn_registry(uacn)
);

CREATE TABLE IF NOT EXISTS teachers (
    uacn VARCHAR(255) PRIMARY KEY,
    staff_id VARCHAR(50),
    department VARCHAR(100)
);

CREATE TABLE IF NOT EXISTS classes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(100) NOT NULL,
    section VARCHAR(10)
);

CREATE TABLE IF NOT EXISTS subjects (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(100) NOT NULL
);

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
