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

    @org.springframework.data.jpa.repository.Query("SELECT COALESCE(SUM(CASE WHEN f.status = 'PAID' THEN f.amountDue ELSE 0 END) / NULLIF(SUM(f.amountDue), 0) * 100, 0.0) FROM FeeInstallment f")
    Double getFeeCollectionRate();
}
