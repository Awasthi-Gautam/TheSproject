package com.example.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Entity
@Table(name = "uacn_registry", schema = "public")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UacnRegistry {
    @Id
    private String uacn;
    private String aadhaarHash;
    private String name;
}
