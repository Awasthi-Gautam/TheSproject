# Docker Setup Guide

This project has two Docker Compose configurations:

## 📋 Configuration Files

### 1. **docker-compose.yml** - Production/Development
- **Purpose**: Clean production-ready environment
- **Profile**: Default (no test profile)
- **Data**: Empty database, manual tenant provisioning
- **Usage**: `docker-compose up`

### 2. **docker-compose.test.yml** - Testing/Demo
- **Purpose**: Automated testing and demonstrations
- **Profile**: `test` (enables TestHydrationRunner)
- **Data**: Auto-populates 2 schools with complete test data
- **Usage**: `docker-compose -f docker-compose.test.yml up`

### 3. **application.properties** - Production Config
- PostgreSQL connection settings
- Multi-tenancy configuration
- No special logging

### 4. **application-test.properties** - Test Config  
- Same database settings
- DEBUG logging for test data hydration
- Used when `SPRING_PROFILES_ACTIVE=test`

---

## 🚀 Usage

### For Manual Testing (Recommended)
```bash
# Start with test data auto-population
docker-compose -f docker-compose.test.yml up --build

# Access APIs at http://localhost:8080
# Test data includes: 2 schools, principals, teachers, students, marks

# Stop and clean up
docker-compose -f docker-compose.test.yml down -v
```

### For Production Deployment
```bash
# Start clean environment
docker-compose up --build

# Provision tenants via API:
POST /api/admin/provision
{
  "schoolName": "Your School",
  "schemaName": "sch_your_school",
  "principalAadhaar": "123456789012",
  "principalName": "Principal Name"
}

# Stop
docker-compose down
```

---

## 🗂️ Test Data Created (Test Profile)

When using `docker-compose.test.yml`, the following data is automatically created:

### Alpha School (`sch_test_alpha`)
- **Principal**: Admin access, manages entire school
- **Teachers**: 3 teachers (Math, Physics, History)
- **Students**: 10 students in Class 10-A
- **Data**: Marks and attendance records

### Beta School (`sch_test_beta`)
- Same structure for cross-tenant testing

---

## 🔧 Customization

### Change Test Data
Edit: `src/main/java/com/example/service/TestDataHydrator.java`

### Modify Database
Edit: `init-db/01-schema.sql` and `init-db/02-school_test.sql`

### Environment Variables
See `docker-compose.yml` and `docker-compose.test.yml` for available env vars
