package com.example.service;

import com.example.domain.*;
import com.example.dto.StudentProfileDTO;
import com.example.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class StudentProfileService {

    private final UacnRegistryRepository uacnRegistryRepository;
    private final StudentRepository studentRepository;
    private final ParentDetailRepository parentDetailRepository;
    private final StudentContactRepository studentContactRepository;
    private final MarksRepository marksRepository;
    private final AttendanceRepository attendanceRepository;
    private final FeeService feeService;

    public StudentProfileService(
            UacnRegistryRepository uacnRegistryRepository,
            StudentRepository studentRepository,
            ParentDetailRepository parentDetailRepository,
            StudentContactRepository studentContactRepository,
            MarksRepository marksRepository,
            AttendanceRepository attendanceRepository,
            FeeService feeService) {
        this.uacnRegistryRepository = uacnRegistryRepository;
        this.studentRepository = studentRepository;
        this.parentDetailRepository = parentDetailRepository;
        this.studentContactRepository = studentContactRepository;
        this.marksRepository = marksRepository;
        this.attendanceRepository = attendanceRepository;
        this.feeService = feeService;
    }

    public StudentProfileDTO getStudentProfile(String uacn) {
        // 1. Basic Info
        UacnRegistry registryEntry = uacnRegistryRepository.findById(uacn)
                .orElseThrow(() -> new RuntimeException("UACN not found in registry: " + uacn));

        Student student = studentRepository.findById(uacn)
                .orElseThrow(() -> new RuntimeException("Student record not found for UACN: " + uacn));

        StudentProfileDTO.BasicInfo basicInfo = new StudentProfileDTO.BasicInfo(
                uacn,
                registryEntry.getName(), // Name comes from public registry
                student.getRollNumber());

        // 2. Contact Info
        Optional<StudentContact> contact = studentContactRepository.findById(uacn);
        List<ParentDetail> parents = parentDetailRepository.findByStudent(student);

        String address = contact.map(StudentContact::getAddress).orElse("N/A");
        String parentPhone = parents.isEmpty() ? "N/A" : parents.get(0).getPrimaryPhone();

        StudentProfileDTO.ContactInfo contactInfo = new StudentProfileDTO.ContactInfo(address, parentPhone);

        // 3. Academic Summary
        List<Marks> marksList = marksRepository.findByUacn(uacn);
        double gpa = calculateGPA(marksList);

        List<Attendance> attendanceList = attendanceRepository.findByUacn(uacn);
        double attendancePercentage = calculateAttendance(attendanceList);

        StudentProfileDTO.AcademicSummary academicSummary = new StudentProfileDTO.AcademicSummary(gpa,
                attendancePercentage);

        // 4. Financial Summary
        BigDecimal totalOutstanding = feeService.calculateTotalOutstandingBalance(uacn);
        StudentProfileDTO.FinancialSummary financialSummary = new StudentProfileDTO.FinancialSummary(totalOutstanding);

        return new StudentProfileDTO(basicInfo, contactInfo, academicSummary, financialSummary);
    }

    private double calculateGPA(List<Marks> marksList) {
        if (marksList.isEmpty())
            return 0.0;
        // Simplified GPA calculation: Average percentage / 10 (just for demo)
        double totalPercentage = marksList.stream()
                .mapToDouble(m -> (double) m.getScoreObtained() / m.getMaxScore() * 100)
                .average()
                .orElse(0.0);
        return totalPercentage / 10.0; // Scale to 10.0
    }

    private double calculateAttendance(List<Attendance> attendanceList) {
        if (attendanceList.isEmpty())
            return 0.0;
        long presentCount = attendanceList.stream()
                .filter(a -> "PRESENT".equalsIgnoreCase(a.getStatus()))
                .count();
        return (double) presentCount / attendanceList.size() * 100.0;
    }
}
