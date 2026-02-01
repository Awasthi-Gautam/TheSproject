package com.example.dto;

import com.fasterxml.jackson.annotation.JsonView;

import java.math.BigDecimal;

public record StudentProfileDTO(
        @JsonView(Views.Public.class) BasicInfo basicInfo,

        @JsonView(Views.Admin.class) ContactInfo contactInfo,

        @JsonView(Views.Public.class) AcademicSummary academicSummary,

        @JsonView(Views.Admin.class) FinancialSummary financialSummary) {
    public record BasicInfo(
            @JsonView(Views.Public.class) String uacn,
            @JsonView(Views.Public.class) String name,
            @JsonView(Views.Public.class) String rollNumber) {
    }

    public record ContactInfo(
            @JsonView(Views.Admin.class) String address,
            @JsonView(Views.Admin.class) String parentPhone) {
    }

    public record AcademicSummary(
            @JsonView(Views.Public.class) Double gpa,
            @JsonView(Views.Public.class) Double attendancePercentage) {
    }

    public record FinancialSummary(
            @JsonView(Views.Admin.class) BigDecimal totalOutstandingFees) {
    }
}
