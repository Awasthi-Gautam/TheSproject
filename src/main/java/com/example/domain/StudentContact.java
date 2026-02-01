package com.example.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.OneToOne;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Entity
@Table(name = "student_contacts")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class StudentContact {

    @Id
    private String uacn;

    @OneToOne
    @MapsId
    @JoinColumn(name = "uacn")
    private Student student;

    private String address;
    private String phone;
    private String email;
    private String emergencyContact;
}
