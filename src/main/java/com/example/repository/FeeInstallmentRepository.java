package com.example.repository;

import com.example.domain.FeeInstallment;
import com.example.domain.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface FeeInstallmentRepository extends JpaRepository<FeeInstallment, UUID> {
    List<FeeInstallment> findByStudent(Student student);

    List<FeeInstallment> findByStudentUacn(String uacn);
}
