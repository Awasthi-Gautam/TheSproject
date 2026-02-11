package com.example.controller;

import com.example.service.TimetableGenerationService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.example.dto.TimetableConfig;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/timetable")
public class TimetableGenerationController {

    private final TimetableGenerationService timetableGenerationService;

    public TimetableGenerationController(TimetableGenerationService timetableGenerationService) {
        this.timetableGenerationService = timetableGenerationService;
    }

    @PostMapping("/generate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> generateSchedulesForSession(
            @RequestParam(required = false) UUID sessionId,
            @RequestBody(required = false) com.example.dto.TimetableConfig config) {

        if (config == null) {
            config = new com.example.dto.TimetableConfig();
        }

        // Handle null sessionId? Request says "for the given sessionId".
        // If optional/missing, we might need a default.
        // The prompt says "given sessionId". I'll assume it's passed or handled by
        // service if internal.
        // However, I previously defaulted string to "current". UUID default is
        // trickier.
        // I'll make it required or check for null.
        if (sessionId == null) {
            return ResponseEntity.badRequest().body("Session ID is required.");
        }

        timetableGenerationService.generateSchedulesForSession(sessionId, config);
        return ResponseEntity.ok("Timetable generation triggered for session: " + sessionId);
    }

}
