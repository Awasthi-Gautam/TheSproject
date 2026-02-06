package com.example.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.UUID;

@Entity
@Table(name = "marks")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Marks {
    @Id
    private UUID id;
    private String uacn;
    private UUID subjectId;
    private Integer scoreObtained;
    private Integer maxScore;
    private String term;
    private UUID academicSessionId;
}
