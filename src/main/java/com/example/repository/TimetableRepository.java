package com.example.repository;

import com.example.domain.Timetable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TimetableRepository extends JpaRepository<Timetable, UUID> {
        List<Timetable> findByTeacherUacn(String teacherUacn);

        List<Timetable> findAllByClassIdOrderByDayOfWeekAscStartTimeAsc(UUID classId);

        List<Timetable> findAllByClassIdAndDayOfWeekOrderByStartTimeAsc(UUID classId, String dayOfWeek);

        List<Timetable> findAllByTeacherUacnOrderByDayOfWeekAscStartTimeAsc(String teacherUacn);

        // Conflict: Teacher booked at same time
        @org.springframework.data.jpa.repository.Query("SELECT COUNT(t) > 0 FROM Timetable t WHERE t.teacherUacn = :teacherUacn AND t.dayOfWeek = :dayOfWeek AND t.startTime < :endTime AND t.endTime > :startTime")
        boolean existsByTeacherConflict(String teacherUacn, String dayOfWeek, java.time.LocalTime startTime,
                        java.time.LocalTime endTime);

        // Conflict: Room booked at same time
        @org.springframework.data.jpa.repository.Query("SELECT COUNT(t) > 0 FROM Timetable t WHERE t.roomNumber = :roomNumber AND t.dayOfWeek = :dayOfWeek AND t.startTime < :endTime AND t.endTime > :startTime")
        boolean existsByRoomConflict(String roomNumber, String dayOfWeek, java.time.LocalTime startTime,
                        java.time.LocalTime endTime);

        // Conflict: Class booked at same time (Student view)
        @org.springframework.data.jpa.repository.Query("SELECT COUNT(t) > 0 FROM Timetable t WHERE t.classId = :classId AND t.dayOfWeek = :dayOfWeek AND t.startTime < :endTime AND t.endTime > :startTime")
        boolean existsByClassConflict(UUID classId, String dayOfWeek, java.time.LocalTime startTime,
                        java.time.LocalTime endTime);

        boolean existsByTeacherUacnAndClassId(String teacherUacn, UUID classId);

        boolean existsByTeacherUacnAndSubjectId(String teacherUacn, UUID subjectId);
}
