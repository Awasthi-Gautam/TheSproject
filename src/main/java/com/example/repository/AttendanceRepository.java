package com.example.repository;

import com.example.domain.Attendance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AttendanceRepository extends JpaRepository<Attendance, UUID> {
    List<Attendance> findByUacn(String uacn);

    @org.springframework.data.jpa.repository.Query("SELECT COALESCE(AVG(CASE WHEN a.status = 'PRESENT' THEN 1.0 ELSE 0.0 END) * 100, 0.0) FROM Attendance a WHERE a.academicSessionId = :sessionId")
    Double getOverallAttendancePercentage(UUID sessionId);

    @org.springframework.data.jpa.repository.Query(value = "SELECT TO_CHAR(a.date, 'Mon') as month, COALESCE(AVG(CASE WHEN a.status = 'PRESENT' THEN 1.0 ELSE 0.0 END) * 100, 0.0) as percentage FROM attendance a WHERE a.academic_session_id = :sessionId GROUP BY TO_CHAR(a.date, 'Mon') ORDER BY MIN(a.date)", nativeQuery = true)
    List<AttendanceTrendProxy> getAttendanceTrends(UUID sessionId);

    interface AttendanceTrendProxy {
        String getMonth();

        Double getPercentage();
    }
}
