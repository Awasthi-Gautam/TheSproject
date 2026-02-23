package com.example.service;

import com.example.dto.AdminDashboardDTO;
import com.example.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminDashboardBffService {

    private final StudentRepository studentRepository;
    private final TeacherRepository teacherRepository;
    private final AttendanceRepository attendanceRepository;
    private final FeeInstallmentRepository feeInstallmentRepository;
    private final SchoolClassRepository schoolClassRepository;

    public AdminDashboardDTO getSchoolOverview(UUID sessionId) {
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {

            // 1. Demographics Task: Count total active students and teachers
            Callable<long[]> demographicsTask = () -> {
                long totalStudents = studentRepository.count();
                long totalTeachers = teacherRepository.count();
                return new long[] { totalStudents, totalTeachers };
            };

            // 2. Attendance KPIs Task: Overall attendance % and trends
            Callable<AttendanceData> attendanceTask = () -> {
                Double overallAttendance = attendanceRepository.getOverallAttendancePercentage(sessionId);
                List<AttendanceRepository.AttendanceTrendProxy> trends = attendanceRepository
                        .getAttendanceTrends(sessionId);

                List<AdminDashboardDTO.AttendanceTrend> trendDtos = trends.stream()
                        .map(t -> new AdminDashboardDTO.AttendanceTrend(t.getMonth(), t.getPercentage()))
                        .toList();

                return new AttendanceData(overallAttendance, trendDtos);
            };

            // 3. Financials Task: Fee collection rate
            Callable<Double> financialsTask = feeInstallmentRepository::getFeeCollectionRate;

            // 4. Class Metrics Task: Class-wise breakdowns
            Callable<List<AdminDashboardDTO.ClassBreakdown>> classMetricsTask = () -> {
                return schoolClassRepository.getClassBreakdowns(sessionId).stream()
                        .map(proxy -> new AdminDashboardDTO.ClassBreakdown(
                                proxy.getClassName(),
                                proxy.getStudentCount(),
                                proxy.getAttendanceRate(),
                                proxy.getClassTeacherName()))
                        .toList();
            };

            // 5. System Alerts Task: Detect operational anomalies
            Callable<List<AdminDashboardDTO.ActionableAlert>> alertsTask = () -> {
                List<AdminDashboardDTO.ActionableAlert> alerts = new ArrayList<>();

                // Alert 1: Classes with no attendance today
                List<String> classesNoAttendance = schoolClassRepository.getClassesWithNoAttendanceToday(sessionId);
                if (!classesNoAttendance.isEmpty()) {
                    alerts.add(new AdminDashboardDTO.ActionableAlert(
                            UUID.randomUUID(),
                            "WARNING",
                            classesNoAttendance.size() + " Classes have no attendance marked today",
                            "/admin/attendance"));
                }

                // Alert 2: Unassigned teachers
                long unassignedTeachers = teacherRepository.countUnassignedTeachers();
                if (unassignedTeachers > 0) {
                    alerts.add(new AdminDashboardDTO.ActionableAlert(
                            UUID.randomUUID(),
                            "INFO",
                            unassignedTeachers + " Teachers are currently unassigned to any class or subject",
                            "/admin/teachers"));
                }

                return alerts;
            };

            // Execute all tasks concurrently
            Future<long[]> demogFuture = executor.submit(demographicsTask);
            Future<AttendanceData> attFuture = executor.submit(attendanceTask);
            Future<Double> finFuture = executor.submit(financialsTask);
            Future<List<AdminDashboardDTO.ClassBreakdown>> classFuture = executor.submit(classMetricsTask);
            Future<List<AdminDashboardDTO.ActionableAlert>> alertsFuture = executor.submit(alertsTask);

            // Await results
            long[] demographics = demogFuture.get();
            AttendanceData attendanceData = attFuture.get();

            AdminDashboardDTO.KpiCards kpiCards = new AdminDashboardDTO.KpiCards(
                    demographics[0],
                    demographics[1],
                    attendanceData.overallAttendance,
                    finFuture.get());

            return new AdminDashboardDTO(
                    kpiCards,
                    attendanceData.trends,
                    classFuture.get(),
                    alertsFuture.get());

        } catch (Exception e) {
            log.error("Failed to construct Admin Dashboard overview for session: {}", sessionId, e);
            throw new RuntimeException("Failed to construct Admin Dashboard overview", e);
        }
    }

    private record AttendanceData(
            Double overallAttendance,
            List<AdminDashboardDTO.AttendanceTrend> trends) {
    }
}
