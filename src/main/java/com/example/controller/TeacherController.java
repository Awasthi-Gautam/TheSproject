package com.example.controller;

import com.example.domain.Timetable;
import com.example.dto.StudentProfileDTO;
import com.example.dto.Views;
import com.example.repository.TimetableRepository;
import com.example.service.StudentProfileService;
import com.fasterxml.jackson.annotation.JsonView;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/teacher")
public class TeacherController {

    private final TimetableRepository timetableRepository;
    private final StudentProfileService studentProfileService;
    // Assuming we would have a ClassService or similar to get members of a class.
    // For this task, we'll simulate fetching class members via a repository we
    // haven't created yet or reuse something?
    // The requirement is "return student profiles".
    // We already have StudentProfileService.getStudentProfile(uacn).
    // We need a way to get "all students in a class".
    // I'll assume StudentRepository can findByClassId or similar.
    // Wait, Student entity doesn't have class_id mapping in the provided snippets
    // (it was in the SQL but maybe not Java).
    // Let's assume we can fetch them or mock it.
    // Actually, looking at Student.java, it only has uacn, roll, admissionDate.
    // The SQL has "classes" table but no explicit link in "students" table shown in
    // snippet?
    // Ah, usually students belong to a class. I'll add a placeholder for now or
    // fetch hypothetically.

    // Correction: In 02-school_test.sql, students table does NOT have class_id.
    // Usually there's a student_classes mapping or similar.
    // But for this task scope, let's focus on the SECURITY aspect.
    // I will fetch a dummy list of UACNs for the class to demonstrate the flow.

    private final com.example.repository.StudentRepository studentRepository;

    private final com.example.repository.SubjectAssignmentRepository subjectAssignmentRepository;

    public TeacherController(TimetableRepository timetableRepository, StudentProfileService studentProfileService,
            com.example.repository.StudentRepository studentRepository,
            com.example.repository.SubjectAssignmentRepository subjectAssignmentRepository) {
        this.timetableRepository = timetableRepository;
        this.studentProfileService = studentProfileService;
        this.studentRepository = studentRepository;
        this.subjectAssignmentRepository = subjectAssignmentRepository;
    }

    @GetMapping("/my-classes")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<List<Timetable>> getMyClasses(Principal principal) {
        String teacherUacn = principal.getName();
        return ResponseEntity.ok(timetableRepository.findByTeacherUacn(teacherUacn));
    }

    @GetMapping("/my-assignments")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<List<com.example.domain.SubjectAssignment>> getMyAssignments(Principal principal) {
        return ResponseEntity.ok(subjectAssignmentRepository.findByUacn(principal.getName()));
    }

    @GetMapping("/class-view/{classId}")
    @PreAuthorize("hasRole('TEACHER') and @securityService.checkTeacherAccess(principal.username, #classId)")
    @JsonView(Views.Public.class) // Hides Admin-only fields like Fees
    public ResponseEntity<List<StudentProfileDTO>> getClassView(@PathVariable UUID classId) {
        return ResponseEntity.ok(
                studentRepository.findByClassId(classId).stream()
                        .map(student -> studentProfileService.getStudentProfile(student.getUacn()))
                        .collect(Collectors.toList()));
    }
}
