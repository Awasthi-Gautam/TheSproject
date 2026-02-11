package com.example.controller;

import com.example.dto.BulkImportResult;
import com.example.service.BulkTeacherService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/admin/teachers")
public class BulkTeacherController {

    private final BulkTeacherService bulkTeacherService;

    public BulkTeacherController(BulkTeacherService bulkTeacherService) {
        this.bulkTeacherService = bulkTeacherService;
    }

    @PostMapping(value = "/bulk-import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> bulkImportTeachers(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "dryRun", defaultValue = "false") boolean dryRun) {

        BulkImportResult result = bulkTeacherService.importTeachers(file, dryRun);

        if (result.isSuccess()) {
            return ResponseEntity.ok("Bulk import successful");
        } else {
            return ResponseEntity.badRequest()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=import_errors.csv")
                    .contentType(MediaType.parseMediaType("text/csv"))
                    .body(result.getErrorCsvContent());
        }
    }
}
