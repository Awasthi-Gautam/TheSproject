package com.example.service;

import com.example.domain.SubjectAssignment;
import com.example.dto.BulkImportResult;
import com.example.repository.SubjectAssignmentRepository;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

@Service
public class FacultyAssignmentService {

    private final SubjectAssignmentRepository subjectAssignmentRepository;

    public FacultyAssignmentService(SubjectAssignmentRepository subjectAssignmentRepository) {
        this.subjectAssignmentRepository = subjectAssignmentRepository;
    }

    @Transactional
    public BulkImportResult bulkAssign(MultipartFile file) {
        try {
            List<CSVRecord> records = validateAndParse(file);
            Queue<SubjectAssignment> successfulAssignments = new ConcurrentLinkedQueue<>();
            Queue<ErrorRow> errorRows = new ConcurrentLinkedQueue<>();

            try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
                List<Future<?>> futures = new ArrayList<>();

                for (CSVRecord record : records) {
                    futures.add(executor.submit(() -> {
                        try {
                            SubjectAssignment assignment = processRow(record);
                            successfulAssignments.add(assignment);
                        } catch (Exception e) {
                            errorRows.add(new ErrorRow(record, e.getMessage()));
                        }
                    }));
                }
                for (Future<?> f : futures) {
                    f.get();
                }
            }

            if (!errorRows.isEmpty()) {
                return BulkImportResult.failure(generateErrorCsv(records, errorRows));
            }

            subjectAssignmentRepository.saveAll(successfulAssignments);
            return BulkImportResult.success();

        } catch (Exception e) {
            throw new RuntimeException("Bulk assignment failed: " + e.getMessage(), e);
        }
    }

    private SubjectAssignment processRow(CSVRecord record) {
        String uacn = record.get("teacher_uacn");
        String classIdStr = record.get("class_id");
        String subjectIdStr = record.get("subject_id");

        if (uacn == null || uacn.isBlank())
            throw new IllegalArgumentException("Missing Teacher UACN");
        if (classIdStr == null || classIdStr.isBlank())
            throw new IllegalArgumentException("Missing Class ID");
        if (subjectIdStr == null || subjectIdStr.isBlank())
            throw new IllegalArgumentException("Missing Subject ID");

        UUID classId = UUID.fromString(classIdStr);
        UUID subjectId = UUID.fromString(subjectIdStr);

        // Check duplicates if needed, but for now we assume overwrite or ignore
        // Or strictly we should check existsByUacnAndClassIdAndSubjectId

        SubjectAssignment assignment = new SubjectAssignment();
        assignment.setId(UUID.randomUUID());
        assignment.setUacn(uacn);
        assignment.setClassId(classId);
        assignment.setSubjectId(subjectId);

        return assignment;
    }

    // Simple parser for now, similar to CsvValidationService but specific
    private List<CSVRecord> validateAndParse(MultipartFile file) throws IOException {
        try (InputStreamReader reader = new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8)) {
            CSVFormat format = CSVFormat.DEFAULT.builder()
                    .setHeader()
                    .setSkipHeaderRecord(true)
                    .setIgnoreHeaderCase(true)
                    .setTrim(true)
                    .build();
            return format.parse(reader).getRecords();
        }
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

    private record ErrorRow(CSVRecord record, String reason) {
    }

    public List<SubjectAssignment> getRoster() {
        return subjectAssignmentRepository.findAll();
    }
}
