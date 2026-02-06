package com.example.repository;

import com.example.domain.AcademicSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface AcademicSessionRepository extends JpaRepository<AcademicSession, UUID> {
}
