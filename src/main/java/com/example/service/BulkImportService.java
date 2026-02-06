package com.example.service;

import com.example.domain.*;
import com.example.dto.BulkImportResult;
import com.example.repository.*;
import jakarta.validation.ConstraintViolationException;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

@Service
public class BulkImportService {

    private final CsvValidationService csvValidationService;
    private final UacnService uacnService;
    private final StudentRepository studentRepository;
    private final ParentDetailRepository parentDetailRepository;
    private final StudentContactRepository studentContactRepository;
    private final AcademicSessionRepository academicSessionRepository;

    public BulkImportService(CsvValidationService csvValidationService, UacnService uacnService,
            StudentRepository studentRepository, ParentDetailRepository parentDetailRepository,
            StudentContactRepository studentContactRepository, AcademicSessionRepository academicSessionRepository) {
        this.csvValidationService = csvValidationService;
        this.uacnService = uacnService;
        this.studentRepository = studentRepository;
        this.parentDetailRepository = parentDetailRepository;
        this.studentContactRepository = studentContactRepository;
        this.academicSessionRepository = academicSessionRepository;
    }

    @Transactional(rollbackFor = { RuntimeException.class })
    public BulkImportResult importStudents(MultipartFile file, UUID sessionId, boolean dryRun) {
        try {
            // 1. Validate and Parse CSV
            List<CSVRecord> records = csvValidationService.validateAndParse(file.getInputStream());

            AcademicSession session = academicSessionRepository.findById(sessionId)
                    .orElseThrow(() -> new IllegalArgumentException("Invalid Academic Session ID"));

            if (!session.isActive()) {
                throw new IllegalArgumentException("Academic Session is not active");
            }

            // 2. Parallel Processing
            Queue<ProcessedData> successfulRows = new ConcurrentLinkedQueue<>();
            Queue<ErrorRow> errorRows = new ConcurrentLinkedQueue<>();

            try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
                List<Future<?>> futures = new ArrayList<>();

                for (CSVRecord record : records) {
                    futures.add(executor.submit(() -> {
                        try {
                            ProcessedData data = processRow(record, session);
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
            for (ProcessedData data : successfulRows) {
                studentRepository.save(data.student);
                if (data.parentDetail != null)
                    parentDetailRepository.save(data.parentDetail);
                if (data.studentContact != null)
                    studentContactRepository.save(data.studentContact);
            }

            return BulkImportResult.success();

        } catch (Exception e) {
            throw new RuntimeException("Bulk import failed: " + e.getMessage(), e);
        }
    }

    private ProcessedData processRow(CSVRecord record, AcademicSession session) {
        String aadhaar = record.get("aadhaar");
        String name = record.get("name");

        // Validate Essential fields
        if (aadhaar == null || aadhaar.isBlank())
            throw new IllegalArgumentException("Missing Aadhaar");
        if (name == null || name.isBlank())
            throw new IllegalArgumentException("Missing Name");

        String uacn = uacnService.resolveUacn(aadhaar, name);

        // Build Student
        Student student = new Student();
        student.setUacn(uacn);
        student.setAcademicSessionId(session.getId());
        student.setClassId(UUID.fromString(record.get("class_id")));

        String dobStr = record.get("dob");
        if (dobStr != null && !dobStr.isBlank()) {
            student.setAdmissionDate(LocalDate.parse(dobStr)); // Using admissionDate as placeholder for DOB logic if
                                                               // needed, or mapped correctly
        }

        // Build Parent Detail
        ParentDetail parent = new ParentDetail();
        parent.setStudent(student);
        parent.setFatherName(record.get("father_name"));
        parent.setMotherName(record.get("mother_name"));

        // Build Contact
        StudentContact contact = new StudentContact();
        contact.setUacn(uacn);
        contact.setStudent(student);
        contact.setPhone(record.get("phone"));
        contact.setEmail(record.get("email"));

        return new ProcessedData(student, parent, contact);
    }

    private String generateErrorCsv(List<CSVRecord> originalRecords, Queue<ErrorRow> errors) throws IOException {
        StringWriter sw = new StringWriter();
        // Get headers from first record or manual list
        // Appending "import_error_reason"

        Map<Long, String> errorMap = new HashMap<>();
        for (ErrorRow er : errors) {
            errorMap.put(er.record.getRecordNumber(), er.reason);
        }

        // Reconstruct CSV with error column
        // Note: commons-csv CSVPrinter requires defined format
        CSVFormat format = CSVFormat.DEFAULT.builder().setHeader().build();

        try (CSVPrinter printer = new CSVPrinter(sw, format)) {
            // Print Header
            List<String> headers = new ArrayList<>(originalRecords.get(0).getParser().getHeaderNames());
            headers.add("import_error_reason");
            printer.printRecord(headers);

            for (CSVRecord record : originalRecords) {
                List<String> values = new ArrayList<>(record.toList());
                if (errorMap.containsKey(record.getRecordNumber())) {
                    values.add(errorMap.get(record.getRecordNumber()));
                } else {
                    values.add(""); // No error for this row, but batch failed
                }
                printer.printRecord(values);
            }
        }
        return sw.toString();
    }

    private record ProcessedData(Student student, ParentDetail parentDetail, StudentContact studentContact) {
    }

    private record ErrorRow(CSVRecord record, String reason) {
    }
}
