package com.example.security;

import com.example.domain.SchoolClass;
import com.example.repository.SchoolClassRepository;
import com.example.repository.SubjectAssignmentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service("accessService")
@Transactional(readOnly = true)
public class AccessService {

    private final SchoolClassRepository schoolClassRepository;
    private final SubjectAssignmentRepository subjectAssignmentRepository;

    public AccessService(SchoolClassRepository schoolClassRepository,
            SubjectAssignmentRepository subjectAssignmentRepository) {
        this.schoolClassRepository = schoolClassRepository;
        this.subjectAssignmentRepository = subjectAssignmentRepository;
    }

    public boolean hasAccess(String uacn, UUID classId, UUID subjectId) {
        // 1. Check if Class Teacher (Access to ALL subjects in their class)
        // We need to fetch the class to check the teacher
        if (isClassTeacher(uacn, classId)) {
            return true;
        }

        // 2. Check if Subject Teacher (Access to specific subject in that class)
        return subjectAssignmentRepository.existsByUacnAndClassIdAndSubjectId(uacn, classId, subjectId);
    }

    private boolean isClassTeacher(String uacn, UUID classId) {
        return schoolClassRepository.findById(classId)
                .map(schoolClass -> uacn.equals(schoolClass.getClassTeacherUacn()))
                .orElse(false);
    }
}
