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
@Table(name = "attendance")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Attendance {
    @Id
    private UUID id;
    private String uacn;
    private LocalDate date;
    private String status;
    private UUID academicSessionId;
}
