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
    private final AuditLogRepository auditLogRepository;

    private static final List<String> DAYS = List.of("MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY");
    private static final int PERIODS_PER_DAY = 8;
    private static final LocalTime START_TIME = LocalTime.of(8, 0);
    private static final int PERIOD_DURATION_MINUTES = 45;
    private static final int BREAK_DURATION_MINUTES = 10; // Assuming short breaks or just consecutive for simplicity

    public TimetableGenerationService(TimetableRepository timetableRepository,
            SubjectAssignmentRepository subjectAssignmentRepository,
            SubjectRepository subjectRepository,
            SchoolClassRepository schoolClassRepository,
            AuditLogRepository auditLogRepository) {
        this.timetableRepository = timetableRepository;
        this.subjectAssignmentRepository = subjectAssignmentRepository;
        this.subjectRepository = subjectRepository;
        this.schoolClassRepository = schoolClassRepository;
        this.auditLogRepository = auditLogRepository;
    }

    public void generateAll(UUID sessionId) {
        generateSchedulesForSession(sessionId, new com.example.dto.TimetableConfig());
    }

    public void generateSchedulesForSession(UUID sessionId, com.example.dto.TimetableConfig config) {
        List<SchoolClass> classes = schoolClassRepository.findAll();

        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            for (SchoolClass cls : classes) {
                executor.submit(() -> generateClassSchedule(cls.getId(), sessionId, config));
            }
        }
    }

    @Transactional
    public void generateClassSchedule(UUID classId, UUID sessionId, com.example.dto.TimetableConfig config) {
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

        // Define Days
        List<String> days = DAYS.subList(0, Math.min(config.getDaysPerWeek(), DAYS.size()));

        int slotsCreated = 0;
        Set<UUID> assignedSubjects = new HashSet<>();

        for (String day : days) {
            subjectDailyCounts.put(day, new HashMap<>());
            LocalTime currentStartTime = START_TIME;

            for (int period = 1; period <= config.getPeriodsPerDay(); period++) {

                // Check Break Slot
                if (period == config.getBreakSlot() + 1) {
                    currentStartTime = currentStartTime.plusMinutes(PERIOD_DURATION_MINUTES + BREAK_DURATION_MINUTES);
                    continue;
                }

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
                    if (timetableRepository.existsByTeacherConflict(teacherUacn, day, currentStartTime,
                            currentEndTime)) {
                        continue;
                    }

                    // Place it!
                    Timetable entry = new Timetable();
                    entry.setId(UUID.randomUUID());
                    entry.setClassId(classId);
                    entry.setSubjectId(subjectId);
                    entry.setTeacherUacn(teacherUacn);
                    entry.setDayOfWeek(day);
                    entry.setStartTime(currentStartTime);
                    entry.setEndTime(currentEndTime);
                    entry.setRoomNumber("TBD");
                    entry.setSessionId(sessionId);
                    entry.setStatus("DRAFT");

                    generatedEntries.add(entry);
                    subjectDailyCounts.get(day).put(subjectId, currentCount + 1);
                    slotsCreated++;
                    assignedSubjects.add(subjectId);
                    break; // Move to next period
                }

                currentStartTime = currentEndTime.plusMinutes(BREAK_DURATION_MINUTES); // Add break or just next slot
            }
        }

        timetableRepository.saveAll(generatedEntries);

        // Identify unresolvable subjects (those that got 0 slots)
        long unresolvableCount = assignments.stream()
                .map(SubjectAssignment::getSubjectId)
                .distinct()
                .filter(id -> !assignedSubjects.contains(id))
                .count();

        // Audit Log
        com.example.domain.AuditLog log = new com.example.domain.AuditLog();
        log.setId(UUID.randomUUID());
        log.setActionByUacn("SYSTEM");
        log.setTargetUacn("CLASS:" + classId);
        log.setActionType("TIMETABLE_GENERATION");
        log.setTimestamp(java.time.LocalDateTime.now());
        // We really want to store details like "Slots: X, Unresolvable: Y".
        // Since AuditLog entity only has basic fields, we might overload 'actionType'
        // or check if there's another field.
        // The entity shows: id, actionByUacn, targetUacn, actionType, timestamp.
        // We will append details to actionType or targetUacn if needed, but cleaner is
        // to keep actionType Enum-like.
        // Let's use targetUacn to store some metadata if possible "CLASS:ID |
        // Slots:X..." or just accept limitation.
        // Request said: "Log the generation event, including the number of slots
        // successfully created and any 'unresolvable' subjects"
        // Without a 'details' column, I'll format actionType carefully or abuse
        // targetUacn slightly.
        // Better: "TIMETABLE_GEN_Slots_" + slotsCreated + "_Unres_" + unresolvableCount
        log.setActionType("TIMETABLE_GEN_OK: Slots=" + slotsCreated + ", Unresolved=" + unresolvableCount);

        auditLogRepository.save(log);
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
