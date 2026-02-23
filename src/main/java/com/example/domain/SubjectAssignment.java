package com.example.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.Column;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.util.UUID;

@Entity
@Table(name = "subject_assignments")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SubjectAssignment {
    @Id
    private UUID id;
    private String uacn;
    private UUID subjectId;
    private UUID classId;
    @Column(name = "academic_session_id")
    private UUID academicSessionId;
}
