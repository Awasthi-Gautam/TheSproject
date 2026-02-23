package com.example.service;

import com.example.domain.*;
import com.example.dto.StudentDashboardDTO;
import com.example.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class StudentDashboardBffService {

    private final StudentRepository studentRepository;
    private final UacnRegistryRepository uacnRegistryRepository;
    private final SchoolClassRepository schoolClassRepository;
    private final AttendanceRepository attendanceRepository;
    private final TimetableRepository timetableRepository;
    private final SubjectRepository subjectRepository;
    private final TeacherRepository teacherRepository;
    private final MarksRepository marksRepository;
    private final FeeInstallmentRepository feeInstallmentRepository;

    public StudentDashboardDTO getDashboardData(String uacn, UUID sessionId) {
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {

            // 1. Profile Task
            Callable<StudentDashboardDTO.ProfileSnapshot> profileTask = () -> {
                Student student = studentRepository.findById(uacn)
                        .orElseThrow(() -> new IllegalArgumentException("Student not found"));
                UacnRegistry registry = uacnRegistryRepository.findById(uacn)
                        .orElseThrow(() -> new IllegalArgumentException("Registry not found"));
                SchoolClass schoolClass = schoolClassRepository.findById(student.getClassId())
                        .orElseThrow(() -> new IllegalArgumentException("Class not found"));

                return new StudentDashboardDTO.ProfileSnapshot(
                        registry.getName(),
                        schoolClass.getName() + " " + schoolClass.getSection(),
                        student.getRollNumber());
            };

            // 2. Attendance Task
            Callable<String> attendanceTask = () -> {
                List<Attendance> attendances = attendanceRepository.findByUacn(uacn);
                if (attendances.isEmpty())
                    return "0%";
                long presentCount = attendances.stream()
                        .filter(a -> "PRESENT".equalsIgnoreCase(a.getStatus()))
                        .count();
                int percentage = (int) ((double) presentCount / attendances.size() * 100);
                return percentage + "%";
            };

            // 3. Timetable Task
            Callable<List<StudentDashboardDTO.ScheduleItem>> timetableTask = () -> {
                Student student = studentRepository.findById(uacn).orElseThrow();
                String todayStr = LocalDate.now().getDayOfWeek().name();
                List<Timetable> timetables = timetableRepository
                        .findAllByClassIdAndDayOfWeekOrderByStartTimeAsc(student.getClassId(), todayStr);

                return timetables.stream().map(t -> {
                    String subjectName = subjectRepository.findById(t.getSubjectId())
                            .map(Subject::getName)
                            .orElse("Unknown");
                    String teacherName = uacnRegistryRepository.findById(t.getTeacherUacn())
                            .map(UacnRegistry::getName)
                            .orElse("Unknown");
                    return new StudentDashboardDTO.ScheduleItem(
                            t.getId(), subjectName, t.getStartTime(), t.getRoomNumber(), teacherName);
                }).collect(Collectors.toList());
            };

            // 4. Marks Task
            Callable<MarksData> marksTask = () -> {
                List<Marks> marksList = marksRepository.findByUacn(uacn);
                if (marksList.isEmpty()) {
                    return new MarksData("N/A", List.of(), List.of());
                }

                List<StudentDashboardDTO.RecentGrade> recentGrades = new ArrayList<>();
                List<StudentDashboardDTO.SubjectPerformance> performances = new ArrayList<>();

                int totalScore = 0;
                int totalMax = 0;

                for (Marks mark : marksList) {
                    totalScore += mark.getScoreObtained();
                    totalMax += mark.getMaxScore();

                    String subjectName = subjectRepository.findById(mark.getSubjectId())
                            .map(Subject::getName).orElse("Unknown");

                    int percentage = (int) ((double) mark.getScoreObtained() / mark.getMaxScore() * 100);
                    String grade = calculateGrade(percentage);

                    performances.add(new StudentDashboardDTO.SubjectPerformance(subjectName, percentage, grade));
                    recentGrades.add(new StudentDashboardDTO.RecentGrade(mark.getId(), subjectName, percentage,
                            LocalDate.now())); // Using LocalDate.now() as placeholder
                }

                String overallGrade = totalMax > 0 ? calculateGrade((int) ((double) totalScore / totalMax * 100))
                        : "N/A";
                return new MarksData(overallGrade, recentGrades, performances);
            };

            // 5. Alerts Task
            Callable<List<StudentDashboardDTO.Alert>> alertsTask = () -> {
                List<FeeInstallment> pendingFees = feeInstallmentRepository.findByStudentUacn(uacn).stream()
                        .filter(f -> "PENDING".equalsIgnoreCase(f.getStatus()))
                        .toList();

                return pendingFees.stream().map(f -> new StudentDashboardDTO.Alert(
                        f.getId(),
                        "WARNING",
                        "Fee Pending: " + f.getCategoryName() + " (Amount: " + f.getAmountDue() + ")",
                        f.getDueDate())).collect(Collectors.toList());
            };

            // Execute all tasks
            Future<StudentDashboardDTO.ProfileSnapshot> profileFuture = executor.submit(profileTask);
            Future<String> attendanceFuture = executor.submit(attendanceTask);
            Future<List<StudentDashboardDTO.ScheduleItem>> timetableFuture = executor.submit(timetableTask);
            Future<MarksData> marksFuture = executor.submit(marksTask);
            Future<List<StudentDashboardDTO.Alert>> alertsFuture = executor.submit(alertsTask);

            // Wait for all results
            MarksData marksData = marksFuture.get();

            StudentDashboardDTO.KpiMetrics kpi = new StudentDashboardDTO.KpiMetrics(
                    attendanceFuture.get(),
                    marksData.overallGrade,
                    0 // Hardcoded pending assignments
            );

            return new StudentDashboardDTO(
                    profileFuture.get(),
                    kpi,
                    alertsFuture.get(),
                    timetableFuture.get(),
                    marksData.recentGrades,
                    marksData.performances);

        } catch (Exception e) {
            log.error("Error fetching dashboard data for student: {}", uacn, e);
            throw new RuntimeException("Failed to load dashboard data", e);
        }
    }

    private String calculateGrade(int percentage) {
        if (percentage >= 90)
            return "A+";
        if (percentage >= 80)
            return "A";
        if (percentage >= 70)
            return "B";
        if (percentage >= 60)
            return "C";
        if (percentage >= 50)
            return "D";
        return "F";
    }

    private record MarksData(
            String overallGrade,
            List<StudentDashboardDTO.RecentGrade> recentGrades,
            List<StudentDashboardDTO.SubjectPerformance> performances) {
    }
}
