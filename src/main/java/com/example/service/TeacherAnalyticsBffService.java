package com.example.service;

import com.example.domain.Marks;
import com.example.domain.SchoolClass;
import com.example.domain.Student;
import com.example.domain.Subject;
import com.example.dto.TeacherClassDashboardDTO;
import com.example.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TeacherAnalyticsBffService {

    private final SchoolClassRepository schoolClassRepository;
    private final SubjectAssignmentRepository subjectAssignmentRepository;
    private final StudentRepository studentRepository;
    private final MarksRepository marksRepository;
    private final AttendanceRepository attendanceRepository;
    private final SubjectRepository subjectRepository;

    @Transactional(readOnly = true)
    public TeacherClassDashboardDTO getClassAnalytics(String teacherUacn, UUID classId, UUID sessionId) {

        // 1. Determine Role (Class Teacher vs Subject Teacher)
        SchoolClass schoolClass = schoolClassRepository.findById(classId)
                .orElseThrow(() -> new IllegalArgumentException("Class not found"));
        boolean isClassTeacher = teacherUacn.equals(schoolClass.getClassTeacherUacn());

        // 2. Fetch Allowable Subjects Data Scope
        List<UUID> allowedSubjectIds;
        if (isClassTeacher) {
            // Class Teacher: Calculate analytics across ALL subjects taught in the class
            // Find all unique subjects assigned to this class
            allowedSubjectIds = subjectAssignmentRepository.findAllByClassId(classId).stream()
                    .map(sa -> sa.getSubjectId())
                    .distinct()
                    .collect(Collectors.toList());
        } else {
            // Subject Teacher: Calculate analytics ONLY on subjects they actually teach in
            // this class
            allowedSubjectIds = subjectAssignmentRepository.findByUacn(teacherUacn).stream()
                    .filter(sa -> sa.getClassId().equals(classId))
                    .map(sa -> sa.getSubjectId())
                    .collect(Collectors.toList());

            if (allowedSubjectIds.isEmpty()) {
                throw new SecurityException("Teacher is not assigned to any subjects in this class");
            }
        }

        // 3. Fetch Class Students
        List<Student> students = studentRepository.findByClassId(classId);

        // 4. Load Scoped Marks
        List<Marks> scopedMarks = marksRepository.findBySessionAndClassAndSubjectIds(sessionId, classId,
                allowedSubjectIds);
        Map<String, List<Marks>> marksByStudent = scopedMarks.stream().collect(Collectors.groupingBy(Marks::getUacn));

        // 5. Build DTO
        List<TeacherClassDashboardDTO.StudentPerformance> studentPerformances = new ArrayList<>();
        double totalClassScorePct = 0.0;
        int classScoreCount = 0;

        for (Student student : students) {
            List<Marks> stMarks = marksByStudent.getOrDefault(student.getUacn(), List.of());

            double studentTotalScore = 0;
            double studentTotalMax = 0;
            List<String> weakSubjects = new ArrayList<>();

            for (Marks m : stMarks) {
                studentTotalScore += m.getScoreObtained();
                studentTotalMax += m.getMaxScore();

                double pct = (double) m.getScoreObtained() / m.getMaxScore() * 100;
                if (pct < 50.0) { // Threshold for "weak" subject
                    String subjName = subjectRepository.findById(m.getSubjectId())
                            .map(Subject::getName).orElse("Unknown");
                    weakSubjects.add(subjName);
                }
            }

            Double overallScore = null;
            if (studentTotalMax > 0) {
                overallScore = (studentTotalScore / studentTotalMax) * 100;
                totalClassScorePct += overallScore;
                classScoreCount++;
            }

            // Using dummy names for demonstration since UserRegistry isn't fetched here
            // individually for speed
            studentPerformances.add(new TeacherClassDashboardDTO.StudentPerformance(
                    student.getUacn(),
                    "Student Name",
                    student.getRollNumber(),
                    overallScore,
                    weakSubjects));
        }

        Double classAverage = classScoreCount > 0 ? (totalClassScorePct / classScoreCount) : 0.0;

        // Use the aggregate SQL call (which we already built for Admin) to get class
        // attendance
        Double classAttendance = attendanceRepository.getOverallAttendancePercentage(sessionId);

        TeacherClassDashboardDTO.ClassOverview overview = new TeacherClassDashboardDTO.ClassOverview(
                schoolClass.getName() + " " + schoolClass.getSection(),
                classAverage,
                (long) students.size(),
                classAttendance);

        return new TeacherClassDashboardDTO(isClassTeacher, overview, studentPerformances);
    }
}
