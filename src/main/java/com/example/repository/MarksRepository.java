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
}
