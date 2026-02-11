package com.example.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.UUID;

@Entity
@Table(name = "timetable")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Timetable {
    @Id
    private UUID id;
    private UUID classId;
    private UUID subjectId;
    private String teacherUacn;
    private String dayOfWeek;
    private java.time.LocalTime startTime;
    private java.time.LocalTime endTime;
    private String roomNumber;
}
