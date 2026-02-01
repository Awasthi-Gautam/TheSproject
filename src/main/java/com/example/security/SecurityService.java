package com.example.security;

import com.example.repository.TimetableRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service("securityService")
@Transactional(readOnly = true)
public class SecurityService {

    private final TimetableRepository timetableRepository;

    public SecurityService(TimetableRepository timetableRepository) {
        this.timetableRepository = timetableRepository;
    }

    public boolean checkTeacherAccess(String teacherUacn, UUID classId) {
        // Check if the teacher has any scheduled class with this classId
        // In a real system you might also check if they are the "Class Teacher"
        // specifically,
        // but here we check if they teach the class at all.
        return timetableRepository.existsByTeacherUacnAndClassId(teacherUacn, classId);
    }

    public boolean checkTeacherSubjectAccess(String teacherUacn, UUID subjectId) {
        return timetableRepository.existsByTeacherUacnAndSubjectId(teacherUacn, subjectId);
    }
}
