package com.example.controller;

import com.example.domain.SubjectAssignment;
import com.example.repository.SubjectAssignmentRepository;
import com.example.service.AdminAssignmentService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminAssignmentController {

    private final AdminAssignmentService adminAssignmentService;
    private final SubjectAssignmentRepository subjectAssignmentRepository;

    public AdminAssignmentController(AdminAssignmentService adminAssignmentService,
            SubjectAssignmentRepository subjectAssignmentRepository) {
        this.adminAssignmentService = adminAssignmentService;
        this.subjectAssignmentRepository = subjectAssignmentRepository;
    }

    @PostMapping("/assign")
    public ResponseEntity<SubjectAssignment> assignSubject(@RequestBody AssignRequest request) {
        SubjectAssignment assignment = adminAssignmentService.assignSubject(request.uacn(), request.classId(),
                request.subjectId());
        return ResponseEntity.ok(assignment);
    }

    @DeleteMapping("/revoke/{id}")
    public ResponseEntity<Void> revokeAssignment(@PathVariable UUID id) {
        adminAssignmentService.revokeAssignment(id);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/staff/{uacn}/permissions")
    public ResponseEntity<List<SubjectAssignment>> getStaffPermissions(@PathVariable String uacn) {
        return ResponseEntity.ok(subjectAssignmentRepository.findByUacn(uacn));
    }

    public record AssignRequest(String uacn, UUID classId, UUID subjectId) {
    }
}
