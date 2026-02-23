package com.example.controller;

import com.example.dto.AdminDashboardDTO;
import com.example.service.AdminDashboardBffService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/dashboard/admin")
@RequiredArgsConstructor
public class AdminDashboardController {

    private final AdminDashboardBffService adminDashboardBffService;

    @GetMapping("/overview")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AdminDashboardDTO> getSchoolOverview(
            @RequestParam(required = false) UUID sessionId) {
        // Fallback to active session logic could be implemented if sessionId is null
        if (sessionId == null) {
            // Depending on architecture, you might fetch an active AcademicSession here
            // from an interceptor/context or require it to be passed.
            // For now, assume it's cleanly passed or handle it gracefully.
        }

        AdminDashboardDTO dashboardData = adminDashboardBffService.getSchoolOverview(sessionId);
        return ResponseEntity.ok(dashboardData);
    }
}
