package com.example.repository;

import com.example.domain.Marks;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface MarksRepository extends JpaRepository<Marks, UUID> {
    List<Marks> findByUacn(String uacn);

    List<Marks> findBySubjectId(UUID subjectId);

    @org.springframework.data.jpa.repository.Query("SELECT m FROM Marks m JOIN Student s ON m.uacn = s.uacn WHERE s.classId = :classId AND m.academicSessionId = :sessionId AND m.subjectId IN :subjectIds")
    List<Marks> findBySessionAndClassAndSubjectIds(UUID sessionId, UUID classId, List<UUID> subjectIds);
}
