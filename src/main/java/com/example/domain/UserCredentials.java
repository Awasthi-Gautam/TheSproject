package com.example.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Entity
@Table(name = "user_credentials")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserCredentials {
    @Id
    private String email; // Username is the email
    private String uacn; // Links to UACN Registry
    private String password; // Hashed or plain (for now temporary)
    private String role; // e.g., ROLE_TEACHER
}
