package com.example.controller;

import com.example.domain.Timetable;
import com.example.service.TimetableService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/timetable")
public class TimetableController {

    private final TimetableService timetableService;

    public TimetableController(TimetableService timetableService) {
        this.timetableService = timetableService;
    }

    @GetMapping("/class/{classId}")
    public ResponseEntity<List<Timetable>> getClassTimetable(@PathVariable UUID classId) {
        return ResponseEntity.ok(timetableService.getClassTimetable(classId));
    }

    @GetMapping("/teacher/{teacherUacn}")
    public ResponseEntity<List<Timetable>> getTeacherTimetable(@PathVariable String teacherUacn) {
        return ResponseEntity.ok(timetableService.getTeacherTimetable(teacherUacn));
    }
}
