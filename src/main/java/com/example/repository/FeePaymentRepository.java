package com.example.repository;

import com.example.domain.FeePayment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;
import java.util.List;

@Repository
public interface FeePaymentRepository extends JpaRepository<FeePayment, UUID> {
    List<FeePayment> findByFeeInstallmentId(UUID installmentId);
}
