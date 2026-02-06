package com.example.readiness;

import com.example.domain.AcademicSession;
import com.example.repository.AcademicSessionRepository;
import com.example.service.BulkImportService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.UUID;

@Component
@Profile("test-bulk-import")
public class BulkImportTester implements CommandLineRunner {

    private final BulkImportService bulkImportService;
    private final AcademicSessionRepository academicSessionRepository;

    public BulkImportTester(BulkImportService bulkImportService, AcademicSessionRepository academicSessionRepository) {
        this.bulkImportService = bulkImportService;
        this.academicSessionRepository = academicSessionRepository;
    }

    @Override
    public void run(String... args) throws Exception {
        System.out.println("--- Starting Bulk Import Test ---");

        // 1. Setup Academic Session
        UUID sessionId = UUID.randomUUID();
        AcademicSession session = new AcademicSession(sessionId, "2025-26",
                LocalDate.of(2025, 4, 1), LocalDate.of(2026, 3, 31), true);
        academicSessionRepository.save(session);
        System.out.println("Created Academic Session: " + sessionId);

        // 2. Mock CSV Data
        String csvData = "aadhaar,name,dob,mother_name,father_name,category_code,status_code,class_id\n" +
                "123456789012,John Doe,2015-01-01,Jane Doe,Bob Doe,GEN,ACTIVE," + UUID.randomUUID() + "\n" +
                "987654321098,Alice Smith,2015-02-01,Mary Smith,Steve Smith,OBC,ACTIVE," + UUID.randomUUID();
        ByteArrayInputStream inputStream = new ByteArrayInputStream(csvData.getBytes(StandardCharsets.UTF_8));

        // 3. Trigger Import
        try {
            bulkImportService.importStudents(inputStream, sessionId);
            System.out.println("Bulk Import Successful!");
        } catch (Exception e) {
            System.err.println("Bulk Import Failed: " + e.getMessage());
            e.printStackTrace();
        }

        System.out.println("--- Bulk Import Test Finished ---");
    }
}
