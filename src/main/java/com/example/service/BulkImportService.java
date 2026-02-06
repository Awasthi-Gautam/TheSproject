package com.example.service;

import com.example.domain.AcademicSession;
import com.example.domain.Student;
import com.example.repository.AcademicSessionRepository;
import com.example.repository.StudentRepository;
import jakarta.validation.ConstraintViolationException;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

@Service
public class BulkImportService {

    private final CsvValidationService csvValidationService;
    private final UacnService uacnService;
    private final StudentRepository studentRepository;
    private final AcademicSessionRepository academicSessionRepository;

    public BulkImportService(CsvValidationService csvValidationService, UacnService uacnService,
            StudentRepository studentRepository, AcademicSessionRepository academicSessionRepository) {
        this.csvValidationService = csvValidationService;
        this.uacnService = uacnService;
        this.studentRepository = studentRepository;
        this.academicSessionRepository = academicSessionRepository;
    }

    @Transactional(rollbackFor = { ConstraintViolationException.class, RuntimeException.class })
    public void importStudents(InputStream csvData, UUID academicSessionId) {
        // 1. Validate and Parse CSV
        List<CSVRecord> records = csvValidationService.validateAndParse(csvData);

        // Verify Academic Session exists
        AcademicSession session = academicSessionRepository.findById(academicSessionId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid Academic Session ID"));

        if (!session.isActive()) {
            throw new IllegalArgumentException("Academic Session is not active");
        }

        // 2. Process rows in parallel using Virtual Threads
        List<Student> studentsToSave = Collections.synchronizedList(new ArrayList<>());
        List<String> errors = Collections.synchronizedList(new ArrayList<>());

        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            List<Future<?>> futures = new ArrayList<>();

            for (CSVRecord record : records) {
                futures.add(executor.submit(() -> {
                    try {
                        String aadhaar = record.get("aadhaar");
                        String name = record.get("name");
                        String dobStr = record.get("dob");
                        String classIdStr = record.get("class_id");

                        // Resolve UACN (External/Shared Service Call)
                        String uacn = uacnService.resolveUacn(aadhaar, name);

                        Student student = new Student();
                        student.setUacn(uacn);
                        student.setAdmissionDate(LocalDate.now()); // Or parse from CSV if needed
                        student.setAcademicSessionId(academicSessionId);
                        student.setClassId(UUID.fromString(classIdStr));
                        student.setRollNumber(generateRollNumber()); // Placeholder logic

                        studentsToSave.add(student);
                    } catch (Exception e) {
                        errors.add("Row " + record.getRecordNumber() + ": " + e.getMessage());
                    }
                }));
            }

            // Wait for all tasks to complete
            for (Future<?> future : futures) {
                try {
                    future.get();
                } catch (InterruptedException | ExecutionException e) {
                    throw new RuntimeException("Error during parallel processing", e);
                }
            }
        }

        if (!errors.isEmpty()) {
            throw new RuntimeException("Bulk import failed with errors: " + String.join(", ", errors));
        }

        // 3. Batch Save (Transactional)
        studentRepository.saveAll(studentsToSave);
    }

    private String generateRollNumber() {
        // Simple placeholder generator
        return String.valueOf(System.nanoTime());
    }
}
