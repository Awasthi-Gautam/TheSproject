package com.example.controller;

import com.example.service.TimetableGenerationService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/timetable")
public class TimetableGenerationController {

    private final TimetableGenerationService timetableGenerationService;

    public TimetableGenerationController(TimetableGenerationService timetableGenerationService) {
        this.timetableGenerationService = timetableGenerationService;
    }

    @PostMapping("/generate/{classId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> generateClassSchedule(@PathVariable UUID classId,
            @RequestParam(defaultValue = "current") String sessionId) {
        timetableGenerationService.generateClassSchedule(classId, sessionId);
        return ResponseEntity.ok("Timetable generation triggered for class " + classId);
    }

    @PostMapping("/generate-all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> generateAll(@RequestParam(defaultValue = "current") String sessionId) {
        timetableGenerationService.generateAll(sessionId);
        return ResponseEntity.ok("Timetable generation triggered for all classes.");
    }
}
