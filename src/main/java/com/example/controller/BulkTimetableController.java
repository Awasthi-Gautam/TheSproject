package com.example.controller;

import com.example.dto.BulkImportResult;
import com.example.service.TimetableService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/admin/timetable")
public class BulkTimetableController {

    private final TimetableService timetableService;

    public BulkTimetableController(TimetableService timetableService) {
        this.timetableService = timetableService;
    }

    @PostMapping(value = "/bulk-import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> bulkImport(
            @RequestParam("file") MultipartFile file) {

        BulkImportResult result = timetableService.bulkImport(file);

        if (result.isSuccess()) {
            return ResponseEntity.ok("Bulk timetable import successful");
        } else {
            return ResponseEntity.badRequest()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=import_errors.csv")
                    .contentType(MediaType.parseMediaType("text/csv"))
                    .body(result.getErrorCsvContent());
        }
    }
}
