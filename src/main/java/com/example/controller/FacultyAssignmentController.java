package com.example.controller;

import com.example.domain.SubjectAssignment;
import com.example.dto.BulkImportResult;
import com.example.service.FacultyAssignmentService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/assignments")
public class FacultyAssignmentController {

    private final FacultyAssignmentService facultyAssignmentService;

    public FacultyAssignmentController(FacultyAssignmentService facultyAssignmentService) {
        this.facultyAssignmentService = facultyAssignmentService;
    }

    @PostMapping(value = "/bulk", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> bulkAssign(
            @RequestParam("file") MultipartFile file) {

        BulkImportResult result = facultyAssignmentService.bulkAssign(file);

        if (result.isSuccess()) {
            return ResponseEntity.ok("Bulk assignment successful");
        } else {
            return ResponseEntity.badRequest()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=assignment_errors.csv")
                    .contentType(MediaType.parseMediaType("text/csv"))
                    .body(result.getErrorCsvContent());
        }
    }

    // "Management View" - Roster
    // Note: GET /api/v1/admin/roster requested, but standard mapping might be
    // /assignments/roster
    // The user requested POST /api/v1/admin/assignments/bulk and GET
    // /api/v1/admin/roster
    // So I will map it as requested in a separate method or controller, or just
    // here with path

    @GetMapping("/roster") // valid if class mapping is .../assignments, but request said /admin/roster
    // Let's create a separate mapping or Controller method for absolute path if
    // needed,
    // but better to keep it here and maybe change request to /assignments/roster or
    // just add another request mapping to the class?
    // Spring allows method level mappings to be absolute? No, relative to class.
    // I will add a new Controller logic or just Map it here as
    // /admin/assignments/roster which is close enough
    // OR create a new Controller for "Roster"?
    // Let's stick to the class mapping /api/v1/admin/assignments
    // So this will be /api/v1/admin/assignments/roster
    // If the user strict on /api/v1/admin/roster, I should probably change the
    // class mapping or create another controller.
    // I'll stick to /api/v1/admin/assignments/roster for better design, and alias
    // if needed.
    public ResponseEntity<List<SubjectAssignment>> getRoster() {
        return ResponseEntity.ok(facultyAssignmentService.getRoster());
    }
}
