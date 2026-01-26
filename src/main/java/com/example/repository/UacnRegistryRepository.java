package com.example.repository;

import com.example.domain.UacnRegistry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UacnRegistryRepository extends JpaRepository<UacnRegistry, String> {
    Optional<UacnRegistry> findByAadhaarHash(String aadhaarHash);
}
