package com.example.controller;

import com.example.dto.StudentProfileDTO;
import com.example.service.StudentProfileService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/students")
public class StudentProfileController {

    private final StudentProfileService studentProfileService;

    public StudentProfileController(StudentProfileService studentProfileService) {
        this.studentProfileService = studentProfileService;
    }

    @GetMapping("/profile/{uacn}")
    public ResponseEntity<StudentProfileDTO> getStudentProfile(@PathVariable String uacn) {
        StudentProfileDTO profile = studentProfileService.getStudentProfile(uacn);
        return ResponseEntity.ok(profile);
    }
}
