package com.example.controller;

import com.example.dto.TeacherClassDashboardDTO;
import com.example.service.TeacherAnalyticsBffService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/dashboard/teacher")
@RequiredArgsConstructor
public class TeacherClassDashboardController {

    private final TeacherAnalyticsBffService analyticsService;

    @GetMapping("/class/{classId}")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public ResponseEntity<TeacherClassDashboardDTO> getClassAnalytics(
            @PathVariable UUID classId,
            @RequestParam UUID sessionId,
            Principal principal) {
        String teacherUacn = principal.getName();
        TeacherClassDashboardDTO dashboard = analyticsService.getClassAnalytics(teacherUacn, classId, sessionId);
        return ResponseEntity.ok(dashboard);
    }
}
