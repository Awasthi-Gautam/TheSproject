package com.example.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.UUID;

@Entity
@Table(name = "organizations", schema = "public")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Organization {
    @Id
    private UUID id;
    private String name;

    @Column(name = "schema_name")
    private String schemaName;
}
