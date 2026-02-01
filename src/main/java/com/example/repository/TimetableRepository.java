package com.example.repository;

import com.example.domain.Timetable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TimetableRepository extends JpaRepository<Timetable, UUID> {
    List<Timetable> findByTeacherUacn(String teacherUacn);

    boolean existsByTeacherUacnAndClassId(String teacherUacn, UUID classId);

    boolean existsByTeacherUacnAndSubjectId(String teacherUacn, UUID subjectId);
}
