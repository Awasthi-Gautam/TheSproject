package com.example.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.Column;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Entity
@Table(name = "teachers")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Teacher {
    @Id
    private String uacn;
    private String staffId;
    private String department;
    private String designation;
    private String email;
    private String phone;
    @Column(name = "employment_type")
    private String employmentType;
    @Column(name = "joining_date")
    private java.time.LocalDate joiningDate;
}
