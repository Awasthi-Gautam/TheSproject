package com.example.dto;

import java.math.BigDecimal;

public record ManagementSummaryDTO(
        long totalEnrollment,
        double overallAttendancePercentage,
        FeeCollectionStatus feeCollectionStatus) {
    public record FeeCollectionStatus(
            BigDecimal totalCollected,
            BigDecimal totalOutstanding) {
    }
}
