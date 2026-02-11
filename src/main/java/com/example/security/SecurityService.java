package com.example.security;

import com.example.repository.SubjectAssignmentRepository;
import com.example.repository.TimetableRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service("securityService")
@Transactional(readOnly = true)
public class SecurityService {

    private final TimetableRepository timetableRepository;
    private final SubjectAssignmentRepository subjectAssignmentRepository;

    public SecurityService(TimetableRepository timetableRepository,
            SubjectAssignmentRepository subjectAssignmentRepository) {
        this.timetableRepository = timetableRepository;
        this.subjectAssignmentRepository = subjectAssignmentRepository;
    }

    public boolean checkTeacherAccess(String teacherUacn, UUID classId) {
        // Check if teacher has ANY subject assignment for this class
        return timetableRepository.existsByTeacherUacnAndClassId(teacherUacn, classId) ||
                subjectAssignmentRepository.existsByUacnAndClassId(teacherUacn, classId);
    }

    public boolean checkTeacherSubjectAccess(String teacherUacn, UUID subjectId) {
        // Note: The main security check is usually done via SubjectAccessAspect which
        // has both ClassID and SubjectID.
        // This method might be used for looser checks.
        return timetableRepository.existsByTeacherUacnAndSubjectId(teacherUacn, subjectId);
    }
}
