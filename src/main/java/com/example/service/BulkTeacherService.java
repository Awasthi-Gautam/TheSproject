package com.example.service;

import com.example.domain.Teacher;
import com.example.domain.UserCredentials;
import com.example.dto.BulkImportResult;
import com.example.repository.TeacherRepository;
import com.example.repository.UserCredentialsRepository;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.StringWriter;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

@Service
public class BulkTeacherService {

    private final CsvValidationService csvValidationService;
    private final UacnService uacnService;
    private final TeacherRepository teacherRepository;
    private final UserCredentialsRepository userCredentialsRepository;

    public BulkTeacherService(CsvValidationService csvValidationService, UacnService uacnService,
            TeacherRepository teacherRepository, UserCredentialsRepository userCredentialsRepository) {
        this.csvValidationService = csvValidationService;
        this.uacnService = uacnService;
        this.teacherRepository = teacherRepository;
        this.userCredentialsRepository = userCredentialsRepository;
    }

    @Transactional(rollbackFor = { RuntimeException.class })
    public BulkImportResult importTeachers(MultipartFile file, boolean dryRun) {
        try {
            // 1. Validate and Parse CSV
            List<CSVRecord> records = csvValidationService.validateAndParse(file.getInputStream(),
                    CsvValidationService.TEACHER_IMPORT_HEADERS);

            // 2. Parallel Processing
            Queue<ProcessedTeacherData> successfulRows = new ConcurrentLinkedQueue<>();
            Queue<ErrorRow> errorRows = new ConcurrentLinkedQueue<>();

            try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
                List<Future<?>> futures = new ArrayList<>();

                for (CSVRecord record : records) {
                    futures.add(executor.submit(() -> {
                        try {
                            ProcessedTeacherData data = processRow(record);
                            successfulRows.add(data);
                        } catch (Exception e) {
                            errorRows.add(new ErrorRow(record, e.getMessage()));
                        }
                    }));
                }

                // Wait for all
                for (Future<?> f : futures) {
                    f.get();
                }
            }

            // 3. Decision Point
            if (!errorRows.isEmpty()) {
                return BulkImportResult.failure(generateErrorCsv(records, errorRows));
            }

            if (dryRun) {
                return BulkImportResult.success();
            }

            // 4. Batch Save
            for (ProcessedTeacherData data : successfulRows) {
                // Check if user credentials already exist for this email to avoid duplicates in
                // public schema check logic
                // For now, assuming email is unique username
                if (userCredentialsRepository.existsById(data.userCredentials.getEmail())) {
                    // In a real bulk scenario, you might want to handle this gracefully (e.g.
                    // update or skip)
                    // But requirement says: "If an email is already taken in the public schema,
                    // flag it as a row error"
                    // Since we process in parallel, we might have missed this check if we only
                    // check at save time.
                    // Ideally, we should check in processRow, but DB access in parallel needs care.
                    // Because of the transactional boundary, we can check here before saving.
                    // If any fail here, we should probably rollback ALL? Or just fail this batch?
                    // Requirement: "Atomic Rollback... If an email is already taken... flag it as a
                    // row error"
                    // This implies we need to fail the whole batch if ANY email is taken, and
                    // report it.
                    // So we should have checked this in processRow or earlier.
                    // Let's refine processRow to check this.
                }

                teacherRepository.save(data.teacher);
                userCredentialsRepository.save(data.userCredentials);
            }

            return BulkImportResult.success();

        } catch (Exception e) {
            throw new RuntimeException("Bulk import failed: " + e.getMessage(), e);
        }
    }

    private ProcessedTeacherData processRow(CSVRecord record) {
        String aadhaar = record.get("aadhaar");
        String name = record.get("name");
        String email = record.get("email");

        // Validate Essential fields
        if (aadhaar == null || aadhaar.isBlank())
            throw new IllegalArgumentException("Missing Aadhaar");
        if (name == null || name.isBlank())
            throw new IllegalArgumentException("Missing Name");
        if (email == null || email.isBlank())
            throw new IllegalArgumentException("Missing Email");

        // Check for existing email (Pseudo-check, real unique constraint should be in
        // DB)
        if (userCredentialsRepository.existsById(email)) {
            throw new IllegalArgumentException("Email already taken: " + email);
        }

        String uacn = uacnService.resolveUacn(aadhaar, name);

        // Build Teacher
        Teacher teacher = new Teacher();
        teacher.setUacn(uacn);
        teacher.setStaffId(record.get("staff_id"));
        teacher.setDepartment(record.get("department"));
        teacher.setDesignation(record.get("designation"));
        teacher.setEmail(email);
        teacher.setPhone(record.get("phone"));

        // Build Create User Credentials
        UserCredentials credentials = new UserCredentials();
        credentials.setEmail(email);
        credentials.setUacn(uacn);
        credentials.setPassword("{noop}welcome123"); // Default temporary password
        credentials.setRole("TEACHER");

        return new ProcessedTeacherData(teacher, credentials);
    }

    private String generateErrorCsv(List<CSVRecord> originalRecords, Queue<ErrorRow> errors) throws IOException {
        StringWriter sw = new StringWriter();
        Map<Long, String> errorMap = new HashMap<>();
        for (ErrorRow er : errors) {
            errorMap.put(er.record.getRecordNumber(), er.reason);
        }

        CSVFormat format = CSVFormat.DEFAULT.builder().setHeader().build();

        try (CSVPrinter printer = new CSVPrinter(sw, format)) {
            List<String> headers = new ArrayList<>(originalRecords.get(0).getParser().getHeaderNames());
            headers.add("import_error_reason");
            printer.printRecord(headers);

            for (CSVRecord record : originalRecords) {
                List<String> values = new ArrayList<>(record.toList());
                if (errorMap.containsKey(record.getRecordNumber())) {
                    values.add(errorMap.get(record.getRecordNumber()));
                } else {
                    values.add("");
                }
                printer.printRecord(values);
            }
        }
        return sw.toString();
    }

    private record ProcessedTeacherData(Teacher teacher, UserCredentials userCredentials) {
    }

    private record ErrorRow(CSVRecord record, String reason) {
    }
}
