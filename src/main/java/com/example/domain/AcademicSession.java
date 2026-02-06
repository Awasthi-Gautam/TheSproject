package com.example.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "academic_sessions")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AcademicSession {
    @Id
    private UUID id;
    private String name; // e.g., "2025-26"
    private LocalDate startDate;
    private LocalDate endDate;
    private boolean isActive;
}
