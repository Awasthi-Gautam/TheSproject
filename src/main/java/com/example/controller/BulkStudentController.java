package com.example.controller;

import com.example.dto.BulkImportResult;
import com.example.service.BulkImportService;
import com.example.aspect.LogAudit; // Assuming annotation exists or will be created/used if available
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/students")
public class BulkStudentController {

    private final BulkImportService bulkImportService;

    public BulkStudentController(BulkImportService bulkImportService) {
        this.bulkImportService = bulkImportService;
    }

    @PostMapping("/bulk-import")
    @PreAuthorize("hasRole('ADMIN')")
    @LogAudit(action = "BULK_IMPORT_STUDENTS")
    public ResponseEntity<?> bulkImport(
            @RequestParam("file") MultipartFile file,
            @RequestParam("sessionId") UUID sessionId,
            @RequestParam(value = "dryRun", defaultValue = "false") boolean dryRun) {

        BulkImportResult result = bulkImportService.importStudents(file, sessionId, dryRun);

        if (result.isSuccess()) {
            return ResponseEntity.ok().body("Bulk import completed successfully.");
        } else {
            return ResponseEntity.badRequest()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=import_errors.csv")
                    .contentType(MediaType.parseMediaType("text/csv"))
                    .body(result.getErrorCsvContent());
        }
    }
}
