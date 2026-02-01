package com.example.service;

import com.example.domain.SubjectAssignment;
import com.example.repository.SubjectAssignmentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional
public class AdminAssignmentService {

    private final SubjectAssignmentRepository subjectAssignmentRepository;

    public AdminAssignmentService(SubjectAssignmentRepository subjectAssignmentRepository) {
        this.subjectAssignmentRepository = subjectAssignmentRepository;
    }

    public SubjectAssignment assignSubject(String uacn, UUID classId, UUID subjectId) {
        if (subjectAssignmentRepository.existsByUacnAndClassIdAndSubjectId(uacn, classId, subjectId)) {
            throw new RuntimeException("Assignment already exists");
        }

        SubjectAssignment assignment = new SubjectAssignment(UUID.randomUUID(), uacn, subjectId, classId);
        return subjectAssignmentRepository.save(assignment);
    }

    public void revokeAssignment(UUID assignmentId) {
        if (!subjectAssignmentRepository.existsById(assignmentId)) {
            throw new RuntimeException("Assignment not found");
        }
        subjectAssignmentRepository.deleteById(assignmentId);
    }
}
