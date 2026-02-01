package com.example.dto;

import java.math.BigDecimal;

public record StudentProfileDTO(
        BasicInfo basicInfo,
        ContactInfo contactInfo,
        AcademicSummary academicSummary,
        FinancialSummary financialSummary) {
    public record BasicInfo(
            String uacn,
            String name,
            String rollNumber) {
    }

    public record ContactInfo(
            String address,
            String parentPhone) {
    }

    public record AcademicSummary(
            Double gpa,
            Double attendancePercentage) {
    }

    public record FinancialSummary(
            BigDecimal totalOutstandingFees) {
    }
}
