package com.example.repository;

import com.example.domain.SchoolClass;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface SchoolClassRepository extends JpaRepository<SchoolClass, UUID> {

    @org.springframework.data.jpa.repository.Query("SELECT c.name || ' ' || c.section as className, " +
            "(SELECT COUNT(s) FROM Student s WHERE s.classId = c.id) as studentCount, " +
            "COALESCE((SELECT AVG(CASE WHEN a.status = 'PRESENT' THEN 1.0 ELSE 0.0 END) * 100 " +
            " FROM Attendance a JOIN Student s ON a.uacn = s.uacn " +
            " WHERE s.classId = c.id AND a.academicSessionId = :sessionId), 0.0) as attendanceRate, " +
            "u.name as classTeacherName " +
            "FROM SchoolClass c LEFT JOIN UacnRegistry u ON c.classTeacherUacn = u.uacn " +
            "WHERE c.academicSessionId = :sessionId")
    java.util.List<ClassBreakdownProxy> getClassBreakdowns(UUID sessionId);

    @org.springframework.data.jpa.repository.Query("SELECT c.name || ' ' || c.section FROM SchoolClass c " +
            "WHERE c.academicSessionId = :sessionId AND NOT EXISTS (" +
            "  SELECT 1 FROM Attendance a JOIN Student s ON a.uacn = s.uacn " +
            "  WHERE s.classId = c.id AND a.date = CURRENT_DATE)")
    java.util.List<String> getClassesWithNoAttendanceToday(UUID sessionId);

    interface ClassBreakdownProxy {
        String getClassName();

        Long getStudentCount();

        Double getAttendanceRate();

        String getClassTeacherName();
    }

    java.util.Optional<com.example.domain.SchoolClass> findByClassTeacherUacnAndAcademicSessionId(String uacn,
            UUID sessionId);
}
