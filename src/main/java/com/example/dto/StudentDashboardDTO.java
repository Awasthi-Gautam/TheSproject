package com.example.dto;

import com.fasterxml.jackson.annotation.JsonView;

import java.util.List;
import java.util.UUID;

public record StudentDashboardDTO(
        ProfileSnapshot profile,
        KpiMetrics kpi,
        @JsonView(Views.StudentOnly.class) List<Alert> alerts,
        List<ScheduleItem> todaySchedule,
        List<RecentGrade> recentGrades,
        List<SubjectPerformance> subjectPerformance) {

    public record ProfileSnapshot(
            String name,
            String grade,
            String rollNumber) {
    }

    public record KpiMetrics(
            String overallAttendance,
            String currentGradeAverage,
            Integer pendingAssignments) {
    }

    public record Alert(
            UUID id,
            String type, // 'WARNING', 'INFO'
            String message,
            java.time.LocalDate date) {
    }

    public record ScheduleItem(
            UUID id,
            String subject,
            java.time.LocalTime time,
            String room,
            String teacherName) {
    }

    public record RecentGrade(
            UUID id,
            String subject,
            Integer grade,
            java.time.LocalDate date) {
    }

    public record SubjectPerformance(
            String subject,
            Integer percentage,
            String grade) {
    }
}
