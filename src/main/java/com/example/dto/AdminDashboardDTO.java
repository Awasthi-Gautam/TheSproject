package com.example.dto;

import java.util.List;
import java.util.UUID;

public record AdminDashboardDTO(
        KpiCards kpiCards,
        List<AttendanceTrend> attendanceTrends,
        List<ClassBreakdown> classBreakdowns,
        List<ActionableAlert> actionableAlerts) {
    public record KpiCards(
            Long totalStudents,
            Long totalTeachers,
            Double overallAttendance,
            Double feeCollectionRate) {
    }

    public record AttendanceTrend(
            String month,
            Double percentage) {
    }

    public record ClassBreakdown(
            String className,
            Long studentCount,
            Double attendanceRate,
            String classTeacherName) {
    }

    public record ActionableAlert(
            UUID id,
            String type, // 'WARNING', 'INFO', 'CRITICAL'
            String message,
            String actionLink) {
    }
}
