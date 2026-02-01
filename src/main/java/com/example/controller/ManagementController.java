package com.example.controller;

import com.example.domain.Attendance;
import com.example.domain.FeeInstallment;
import com.example.domain.FeePayment;
import com.example.dto.ManagementSummaryDTO;
import com.example.repository.AttendanceRepository;
import com.example.repository.FeeInstallmentRepository;
import com.example.repository.FeePaymentRepository;
import com.example.repository.StudentRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/mgmt")
public class ManagementController {

    private final StudentRepository studentRepository;
    private final AttendanceRepository attendanceRepository;
    private final FeeInstallmentRepository feeInstallmentRepository;
    private final FeePaymentRepository feePaymentRepository;

    public ManagementController(
            StudentRepository studentRepository,
            AttendanceRepository attendanceRepository,
            FeeInstallmentRepository feeInstallmentRepository,
            FeePaymentRepository feePaymentRepository) {
        this.studentRepository = studentRepository;
        this.attendanceRepository = attendanceRepository;
        this.feeInstallmentRepository = feeInstallmentRepository;
        this.feePaymentRepository = feePaymentRepository;
    }

    @GetMapping("/summary")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ManagementSummaryDTO> getSummary() {
        // 1. Total Enrollment
        long totalEnrollment = studentRepository.count();

        // 2. Overall Attendance
        List<Attendance> allAttendance = attendanceRepository.findAll();
        double overallAttendance = 0.0;
        if (!allAttendance.isEmpty()) {
            long presentCount = allAttendance.stream()
                    .filter(a -> "PRESENT".equalsIgnoreCase(a.getStatus()))
                    .count();
            overallAttendance = (double) presentCount / allAttendance.size() * 100.0;
        }

        // 3. Fee Collection Status
        List<FeeInstallment> installments = feeInstallmentRepository.findAll();
        List<FeePayment> payments = feePaymentRepository.findAll();

        BigDecimal totalDue = installments.stream()
                .map(FeeInstallment::getAmountDue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalPaid = payments.stream()
                .map(FeePayment::getAmountPaid)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalOutstanding = totalDue.subtract(totalPaid);

        ManagementSummaryDTO.FeeCollectionStatus feeStatus = new ManagementSummaryDTO.FeeCollectionStatus(totalPaid,
                totalOutstanding);

        return ResponseEntity.ok(new ManagementSummaryDTO(totalEnrollment, overallAttendance, feeStatus));
    }
}
