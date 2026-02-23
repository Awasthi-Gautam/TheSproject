package com.example.dto;

import java.util.List;
import java.util.UUID;

public record TeacherClassDashboardDTO(
        boolean isClassTeacherView,
        ClassOverview classOverview,
        List<StudentPerformance> students) {
    public record ClassOverview(
            String className,
            Double classAverage,
            Long totalStudents,
            Double classAttendance) {
    }

    public record StudentPerformance(
            String uacn,
            String name,
            String rollNumber,
            Double overallScore,
            List<String> weakSubjects // Specific subjects failing below a threshold
    ) {
    }
}
