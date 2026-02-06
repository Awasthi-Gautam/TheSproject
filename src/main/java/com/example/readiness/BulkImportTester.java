package com.example.readiness;

import com.example.domain.AcademicSession;
import com.example.repository.AcademicSessionRepository;
import com.example.service.BulkImportService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

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
            // Simple MultipartFile implementation for testing
            MultipartFile multipartFile = new MockMultipartFile("file", "students.csv", "text/csv",
                    csvData.getBytes(StandardCharsets.UTF_8));

            var result = bulkImportService.importStudents(multipartFile, sessionId, false);

            if (result.isSuccess()) {
                System.out.println("Bulk Import Successful!");
            } else {
                System.err.println("Bulk Import Failed with errors:\n" + result.getErrorCsvContent());
            }

        } catch (Exception e) {
            System.err.println("Bulk Import Failed: " + e.getMessage());
            e.printStackTrace();
        }

        System.out.println("--- Bulk Import Test Finished ---");
    }

    // Minimal implementation of MultipartFile for testing purposes within src/main
    private static class MockMultipartFile implements MultipartFile {
        private final String name;
        private final String originalFilename;
        private final String contentType;
        private final byte[] content;

        public MockMultipartFile(String name, String originalFilename, String contentType, byte[] content) {
            this.name = name;
            this.originalFilename = originalFilename;
            this.contentType = contentType;
            this.content = content;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public String getOriginalFilename() {
            return originalFilename;
        }

        @Override
        public String getContentType() {
            return contentType;
        }

        @Override
        public boolean isEmpty() {
            return content.length == 0;
        }

        @Override
        public long getSize() {
            return content.length;
        }

        @Override
        public byte[] getBytes() throws java.io.IOException {
            return content;
        }

        @Override
        public java.io.InputStream getInputStream() throws java.io.IOException {
            return new java.io.ByteArrayInputStream(content);
        }

        @Override
        public void transferTo(java.io.File dest) throws java.io.IOException, IllegalStateException {
            throw new UnsupportedOperationException();
        }
    }
}
