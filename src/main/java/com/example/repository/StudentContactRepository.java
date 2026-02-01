package com.example.repository;

import com.example.domain.StudentContact;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface StudentContactRepository extends JpaRepository<StudentContact, String> {
}
