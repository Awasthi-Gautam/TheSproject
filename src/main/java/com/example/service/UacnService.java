package com.example.service;

import com.example.domain.Student;
import com.example.domain.UacnRegistry;
import com.example.repository.StudentRepository;
import com.example.repository.UacnRegistryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.util.UUID;

@Service
public class UacnService {

    private final UacnRegistryRepository uacnRegistryRepository;
    private final StudentRepository studentRepository;

    public UacnService(UacnRegistryRepository uacnRegistryRepository, StudentRepository studentRepository) {
        this.uacnRegistryRepository = uacnRegistryRepository;
        this.studentRepository = studentRepository;
    }

    @Transactional
    public String onboardStudent(String aadhaar, String name) {
        String aadhaarHash = hashAadhaar(aadhaar);

        UacnRegistry registryEntry = uacnRegistryRepository.findByAadhaarHash(aadhaarHash)
                .orElseGet(() -> {
                    String newUacn = generateRandomUacn();
                    UacnRegistry newEntry = new UacnRegistry(newUacn, aadhaarHash, name);
                    return uacnRegistryRepository.save(newEntry);
                });

        // Check if student already exists in this tenant (optional, but good practice)
        if (studentRepository.existsById(registryEntry.getUacn())) {
            // Already registered in this school
            return registryEntry.getUacn();
        }

        Student student = new Student();
        student.setUacn(registryEntry.getUacn());
        student.setAdmissionDate(LocalDate.now());
        // Set other default fields if necessary

        studentRepository.save(student);

        return registryEntry.getUacn();
    }

    private String hashAadhaar(String aadhaar) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] encodedhash = digest.digest(aadhaar.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(encodedhash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not supported", e);
        }
    }

    private String bytesToHex(byte[] hash) {
        StringBuilder hexString = new StringBuilder(2 * hash.length);
        for (int i = 0; i < hash.length; i++) {
            String hex = Integer.toHexString(0xff & hash[i]);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }

    // Public method to get or create UACN for a given Aadhaar (used for
    // Principals/Teachers)
    @Transactional
    public String generateUacn(String aadhaar) {
        String aadhaarHash = hashAadhaar(aadhaar);
        return uacnRegistryRepository.findByAadhaarHash(aadhaarHash)
                .map(UacnRegistry::getUacn)
                .orElseGet(() -> {
                    String newUacn = generateRandomUacn();
                    UacnRegistry newEntry = new UacnRegistry(newUacn, aadhaarHash, "Principal/Teacher"); // Name isn't
                                                                                                         // passed here,
                                                                                                         // maybe update
                                                                                                         // later
                    uacnRegistryRepository.save(newEntry);
                    return newUacn;
                });
    }

    // Public method to resolving UACN without saving to tenant student table
    // immediately
    @Transactional
    public String resolveUacn(String aadhaar, String name) {
        String aadhaarHash = hashAadhaar(aadhaar);

        // Check if registry entry exists
        return uacnRegistryRepository.findByAadhaarHash(aadhaarHash)
                .map(UacnRegistry::getUacn)
                .orElseGet(() -> {
                    // Create new registry entry if not found
                    String newUacn = generateRandomUacn();
                    UacnRegistry newEntry = new UacnRegistry(newUacn, aadhaarHash, name);
                    uacnRegistryRepository.save(newEntry);
                    return newUacn;
                });
    }

    // Simple UACN generator for demo purposes
    private String generateRandomUacn() {
        return "UACN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}
