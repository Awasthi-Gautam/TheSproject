-- Create the public schema if it doesn't exist (usually it does)
CREATE SCHEMA IF NOT EXISTS public;

-- Organizations table
CREATE TABLE IF NOT EXISTS public.organizations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    schema_name VARCHAR(255) NOT NULL UNIQUE
);

-- UACN Registry table
CREATE TABLE IF NOT EXISTS public.uacn_registry (
    uacn VARCHAR(255) PRIMARY KEY,
    aadhaar_hash VARCHAR(255) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL
);

-- Organization Memberships table
CREATE TABLE IF NOT EXISTS public.org_memberships (
    uacn VARCHAR(255) NOT NULL,
    org_id UUID NOT NULL,
    role VARCHAR(50) NOT NULL,
    PRIMARY KEY (uacn, org_id),
    FOREIGN KEY (uacn) REFERENCES public.uacn_registry(uacn),
    FOREIGN KEY (org_id) REFERENCES public.organizations(id)
);

-- Insert a demo organization
INSERT INTO public.organizations (name, schema_name) VALUES ('Test School', 'school_test') ON CONFLICT DO NOTHING;
