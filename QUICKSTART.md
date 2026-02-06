# Quick Reference

## Start Testing Environment
```bash
docker-compose -f docker-compose.test.yml up --build
```
**Includes**: Auto test data (2 schools, teachers, students, marks)

## Start Production Environment  
```bash
docker-compose up --build
```
**Includes**: Empty database, manual provisioning needed

## Stop and Clean
```bash
# Stop test environment
docker-compose -f docker-compose.test.yml down -v

# Stop production environment
docker-compose down -v
```

## View Logs
```bash
# Test environment
docker-compose -f docker-compose.test.yml logs -f app

# Production environment
docker-compose logs -f app
```

## Access Database
```bash
# While running, connect to PostgreSQL
docker exec -it showfolio-test-db psql -U postgres -d showfolio_db

# List tables
\dt

# View test data
SELECT * FROM uacn_registry LIMIT 10;
```

## Test API
```bash
# After test environment starts, get a student profile
# Replace STUDENT_UACN with actual UACN from database
curl -X GET "http://localhost:8080/api/v1/students/profile/STUDENT_UACN" \
  -H "X-Tenant-ID: sch_test_alpha" \
  -u "PRINCIPAL_UACN:password"
```

See **MANUAL_TESTING.md** for detailed API examples.
