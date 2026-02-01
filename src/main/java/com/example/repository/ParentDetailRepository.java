package com.example.repository;

import com.example.domain.ParentDetail;
import com.example.domain.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;
import java.util.List;

@Repository
public interface ParentDetailRepository extends JpaRepository<ParentDetail, UUID> {
    List<ParentDetail> findByStudent(Student student);
}
