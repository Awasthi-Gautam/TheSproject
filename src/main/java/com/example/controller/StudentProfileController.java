package com.example.controller;

import com.example.dto.StudentProfileDTO;
import com.example.service.StudentProfileService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/students")
public class StudentProfileController {

    private final StudentProfileService studentProfileService;
    private final ObjectMapper objectMapper;

    public StudentProfileController(StudentProfileService studentProfileService, ObjectMapper objectMapper) {
        this.studentProfileService = studentProfileService;
        this.objectMapper = objectMapper;
    }

    @GetMapping(value = "/profile/{uacn}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> getStudentProfile(@PathVariable String uacn,
            Authentication authentication) throws JsonProcessingException {
        StudentProfileDTO profile = studentProfileService.getStudentProfile(uacn);

        // Determine the view class based on user authorities
        Class<?> viewClass;
        if (authentication.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))) {
            viewClass = com.example.dto.Views.Admin.class;
        } else if (authentication.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_TEACHER"))) {
            viewClass = com.example.dto.Views.Teacher.class;
        } else {
            viewClass = com.example.dto.Views.Public.class;
        }

        // Serialize with the appropriate view
        String json = objectMapper.writerWithView(viewClass).writeValueAsString(profile);

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(json);
    }
}
