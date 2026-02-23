package com.example.controller;

import com.example.dto.StudentDashboardDTO;
import com.example.dto.Views;
import com.example.service.StudentDashboardBffService;
import com.fasterxml.jackson.annotation.JsonView;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor
public class StudentDashboardController {

    private final StudentDashboardBffService dashboardBffService;

    @GetMapping("/student/{uacn}")
    @PreAuthorize("hasAnyRole('ADMIN', 'CLASS_TEACHER', 'PARENT') or #uacn == authentication.name")
    @JsonView(Views.StudentOnly.class)
    public ResponseEntity<StudentDashboardDTO> getStudentDashboard(
            @PathVariable String uacn,
            @RequestParam(required = false) UUID sessionId) {
        StudentDashboardDTO dashboardData = dashboardBffService.getDashboardData(uacn, sessionId);
        return ResponseEntity.ok(dashboardData);
    }

    @GetMapping("/teacher-view/student/{uacn}")
    @PreAuthorize("hasAnyRole('ADMIN', 'CLASS_TEACHER')")
    @JsonView(Views.Public.class)
    public ResponseEntity<StudentDashboardDTO> getStudentDashboardTeacherView(
            @PathVariable String uacn,
            @RequestParam(required = false) UUID sessionId) {
        StudentDashboardDTO dashboardData = dashboardBffService.getDashboardData(uacn, sessionId);
        return ResponseEntity.ok(dashboardData);
    }
}
