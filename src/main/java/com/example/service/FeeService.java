package com.example.service;

import com.example.domain.FeeInstallment;
import com.example.domain.FeePayment;
import com.example.repository.FeeInstallmentRepository;
import com.example.repository.FeePaymentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class FeeService {

    private final FeeInstallmentRepository feeInstallmentRepository;
    private final FeePaymentRepository feePaymentRepository;

    public FeeService(FeeInstallmentRepository feeInstallmentRepository, FeePaymentRepository feePaymentRepository) {
        this.feeInstallmentRepository = feeInstallmentRepository;
        this.feePaymentRepository = feePaymentRepository;
    }

    /**
     * Calculates the total outstanding balance for a given student UACN.
     * Total Outstanding = Sum(FeeInstallment.amountDue) -
     * Sum(FeePayment.amountPaid)
     *
     * @param uacn The Unique Access Control Number of the student.
     * @return The total outstanding balance.
     */
    public BigDecimal calculateTotalOutstandingBalance(String uacn) {
        List<FeeInstallment> installments = feeInstallmentRepository.findByStudentUacn(uacn);

        BigDecimal totalDue = BigDecimal.ZERO;
        BigDecimal totalPaid = BigDecimal.ZERO;

        for (FeeInstallment installment : installments) {
            totalDue = totalDue.add(installment.getAmountDue());

            List<FeePayment> payments = feePaymentRepository.findByFeeInstallmentId(installment.getId());
            for (FeePayment payment : payments) {
                totalPaid = totalPaid.add(payment.getAmountPaid());
            }
        }

        return totalDue.subtract(totalPaid);
    }
}
