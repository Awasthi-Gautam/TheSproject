package com.example.service;

import com.example.domain.Subject;
import com.example.domain.SubjectAssignment;
import com.example.domain.Timetable;
import com.example.domain.SchoolClass;
import com.example.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

@Service
public class TimetableGenerationService {

    private final TimetableRepository timetableRepository;
    private final SubjectAssignmentRepository subjectAssignmentRepository;
    private final SubjectRepository subjectRepository;
    private final SchoolClassRepository schoolClassRepository;

    private static final List<String> DAYS = List.of("MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY");
    private static final int PERIODS_PER_DAY = 8;
    private static final LocalTime START_TIME = LocalTime.of(8, 0);
    private static final int PERIOD_DURATION_MINUTES = 45;
    private static final int BREAK_DURATION_MINUTES = 10; // Assuming short breaks or just consecutive for simplicity

    public TimetableGenerationService(TimetableRepository timetableRepository,
            SubjectAssignmentRepository subjectAssignmentRepository,
            SubjectRepository subjectRepository,
            SchoolClassRepository schoolClassRepository) {
        this.timetableRepository = timetableRepository;
        this.subjectAssignmentRepository = subjectAssignmentRepository;
        this.subjectRepository = subjectRepository;
        this.schoolClassRepository = schoolClassRepository;
    }

    public void generateAll(String sessionId) {
        // Find all classes (optionally filter by session if SchoolClass has session
        // link,
        // but for now findAll() as per current SchoolClass definition checks)
        // If SchoolClass has sessionId, we should filter.
        // Assuming all active classes for now.
        List<SchoolClass> classes = schoolClassRepository.findAll();

        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            for (SchoolClass cls : classes) {
                executor.submit(() -> generateClassSchedule(cls.getId(), sessionId));
            }
        }
    }

    @Transactional
    public void generateClassSchedule(UUID classId, String sessionId) {
        // 1. Fetch Assignments
        List<SubjectAssignment> assignments = subjectAssignmentRepository.findAllByClassId(classId);
        if (assignments.isEmpty())
            return;

        // 2. Fetch Subject Details and Sort (Core First)
        Map<UUID, Subject> subjectMap = subjectRepository.findAllById(
                assignments.stream().map(SubjectAssignment::getSubjectId).collect(Collectors.toSet())).stream()
                .collect(Collectors.toMap(Subject::getId, s -> s));

        List<SubjectAssignment> sortedAssignments = new ArrayList<>(assignments);
        sortedAssignments.sort((a, b) -> {
            boolean aCore = isCore(subjectMap.get(a.getSubjectId()));
            boolean bCore = isCore(subjectMap.get(b.getSubjectId()));
            return Boolean.compare(bCore, aCore); // True (Core) first
        });

        // 3. Generate Structure
        List<Timetable> generatedEntries = new ArrayList<>();
        Map<String, Map<UUID, Integer>> subjectDailyCounts = new HashMap<>(); // Day -> SubjectID -> Count

        for (String day : DAYS) {
            subjectDailyCounts.put(day, new HashMap<>());
            LocalTime currentStartTime = START_TIME;

            for (int period = 1; period <= PERIODS_PER_DAY; period++) {
                LocalTime currentEndTime = currentStartTime.plusMinutes(PERIOD_DURATION_MINUTES);

                // Attempt to place a subject
                for (SubjectAssignment assignment : sortedAssignments) {
                    UUID subjectId = assignment.getSubjectId();
                    String teacherUacn = assignment.getUacn();

                    // Check Constraints
                    // A. Max 2 per day
                    int currentCount = subjectDailyCounts.get(day).getOrDefault(subjectId, 0);
                    if (currentCount >= 2)
                        continue;

                    // B. Teacher Conflict (Global Check)
                    // We check if teacher is busy in existing 'Live' or 'Draft' timetables.
                    if (timetableRepository.existsByTeacherConflict(teacherUacn, day, currentStartTime,
                            currentEndTime)) {
                        continue;
                    }

                    // C. Class Conflict (Already filled? We are building sequentially so this slot
                    // is empty effectively, but just in case)
                    // Logic here implies we only fill empty slots.

                    // Place it!
                    Timetable entry = new Timetable();
                    entry.setId(UUID.randomUUID());
                    entry.setClassId(classId);
                    entry.setSubjectId(subjectId);
                    entry.setTeacherUacn(teacherUacn);
                    entry.setDayOfWeek(day);
                    entry.setStartTime(currentStartTime);
                    entry.setEndTime(currentEndTime);
                    entry.setDraft(true); // Draft Mode

                    generatedEntries.add(entry);
                    subjectDailyCounts.get(day).put(subjectId, currentCount + 1);
                    break; // Move to next period
                }

                currentStartTime = currentEndTime.plusMinutes(BREAK_DURATION_MINUTES); // Add break or just next slot
            }
        }

        timetableRepository.saveAll(generatedEntries);
    }

    private boolean isCore(Subject subject) {
        if (subject == null || subject.getName() == null)
            return false;
        String name = subject.getName().toLowerCase();
        return name.contains("math") || name.contains("science") ||
                name.contains("physics") || name.contains("chemistry") ||
                name.contains("biology") || name.contains("english");
    }
}
