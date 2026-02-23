package com.example.repository;

import com.example.domain.Teacher;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TeacherRepository extends JpaRepository<Teacher, String> {

    @org.springframework.data.jpa.repository.Query("SELECT COUNT(t) FROM Teacher t WHERE t.uacn NOT IN (SELECT c.classTeacherUacn FROM SchoolClass c WHERE c.classTeacherUacn IS NOT NULL) AND t.uacn NOT IN (SELECT sa.uacn FROM SubjectAssignment sa)")
    long countUnassignedTeachers();
}
