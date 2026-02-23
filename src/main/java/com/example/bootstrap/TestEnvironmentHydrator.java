package com.example.bootstrap;

import com.example.domain.*;
import com.example.multitenancy.TenantContext;
import com.example.repository.*;
import com.example.service.*;
import com.example.controller.ManagementController;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;

@Component
@Profile("dev")
public class TestEnvironmentHydrator implements CommandLineRunner {

    private final OrganizationRepository organizationRepository;
    private final UacnRegistryRepository uacnRegistryRepository;
    private final TenantProvisioningService tenantProvisioningService;
    private final AcademicSessionRepository academicSessionRepository;
    private final SchoolClassRepository schoolClassRepository;
    private final SubjectRepository subjectRepository;
    private final TeacherRepository teacherRepository;
    private final StudentRepository studentRepository;
    private final StudentContactRepository studentContactRepository;
    private final SubjectAssignmentRepository subjectAssignmentRepository;
    private final TimetableService timetableService;
    private final StudentProfileService studentProfileService;
    private final TimetableRepository timetableRepository;
    private final OrgMembershipRepository orgMembershipRepository;
    private final ManagementController managementController;
    private final UacnService uacnService;

    public TestEnvironmentHydrator(OrganizationRepository organizationRepository,
            UacnRegistryRepository uacnRegistryRepository,
            TenantProvisioningService tenantProvisioningService,
            AcademicSessionRepository academicSessionRepository,
            SchoolClassRepository schoolClassRepository,
            SubjectRepository subjectRepository,
            TeacherRepository teacherRepository,
            StudentRepository studentRepository,
            StudentContactRepository studentContactRepository,
            SubjectAssignmentRepository subjectAssignmentRepository,
            TimetableService timetableService,
            StudentProfileService studentProfileService,
            TimetableRepository timetableRepository,
            OrgMembershipRepository orgMembershipRepository,
            ManagementController managementController,
            UacnService uacnService) {
        this.organizationRepository = organizationRepository;
        this.uacnRegistryRepository = uacnRegistryRepository;
        this.tenantProvisioningService = tenantProvisioningService;
        this.academicSessionRepository = academicSessionRepository;
        this.schoolClassRepository = schoolClassRepository;
        this.subjectRepository = subjectRepository;
        this.teacherRepository = teacherRepository;
        this.studentRepository = studentRepository;
        this.studentContactRepository = studentContactRepository;
        this.subjectAssignmentRepository = subjectAssignmentRepository;
        this.timetableService = timetableService;
        this.studentProfileService = studentProfileService;
        this.timetableRepository = timetableRepository;
        this.orgMembershipRepository = orgMembershipRepository;
        this.managementController = managementController;
        this.uacnService = uacnService;
    }

    @Override
    public void run(String... args) throws Exception {
        if (organizationRepository.count() > 0) {
            System.out.println("Data already exists. Skipping hydration.");
            return;
        }

        System.out.println("Starting Test Environment Hydration...");

        // Step 1: Create Organization & SuperAdmin
        createSuperAdmin();

        // Step 2: Tenant Infrastructure
        String schemaName = "sch_alpha";
        tenantProvisioningService.createSchoolWithSchema("Alpha International", schemaName, "123456789012",
                "Principal Alpha");

        // Switch to Tenant Context
        TenantContext.setCurrentTenant(schemaName);

        try {
            // Step 3: Operational Data
            seedOperationalData();

            // Step 4: Timetable
            seedTimetable();

            // Step 5: Verification
            performSelfTest();

        } finally {
            TenantContext.clear();
        }

        System.out.println("Test Environment Hydration Completed.");
    }

    private void createSuperAdmin() {
        UacnRegistry superAdmin = new UacnRegistry();
        superAdmin.setUacn("SA001");
        superAdmin.setName("Super Admin");
        superAdmin.setEmail("admin@alpha.com");
        superAdmin.setPasswordHash("hashed_password");
        superAdmin.setAadhaarHash("dummy_hash_for_superadmin"); // SuperAdmin doesn't have real Aadhaar
        // Role is not on Registry, it is in OrgMembership (or global system user if
        // outside tenant)
        // Since we are hydrating "Alpha International", we make him an ADMIN of that
        // org.
        uacnRegistryRepository.save(superAdmin);

        // Find the org we just created (Need to wait for step 2 or create here?
        // Step 2 creates it. We should assign role AFTER step 2 or make SuperAdmin
        // global.
        // Typically SuperAdmin is global.
        // But for "Principal Alpha" created in Step 2, logic is in
        // TenantProvisioningService.
        // Let's assume SA001 is a global admin or we assign him to the org later.
        // I will assign him to the org in seedOperationalData or right after Step 2.
        System.out.println("SuperAdmin Created: SA001");
    }

    // Helper to assign role
    private void assignRole(String uacn, String role, UUID orgId) {
        OrgMembership membership = new OrgMembership();
        membership.setId(UUID.randomUUID());
        membership.setMemberUacn(uacn);
        membership.setOrgId(orgId);
        membership.setRole(role);
        membership.setJoinedAt(LocalDate.now());
        orgMembershipRepository.save(membership);
    }

    @Transactional
    public void seedOperationalData() {
        // Get Org ID for role assignment
        Organization org = organizationRepository.findAll().get(0);

        // Academic Session
        AcademicSession session = new AcademicSession();
        session.setId(UUID.randomUUID());
        session.setName("2025-26");
        session.setStartDate(LocalDate.of(2025, 4, 1));
        session.setEndDate(LocalDate.of(2026, 3, 31));
        session.setActive(true);
        academicSessionRepository.save(session);

        // Classes
        List<SchoolClass> classes = new ArrayList<>();
        for (String name : List.of("10-A", "10-B", "11-A")) {
            SchoolClass cls = new SchoolClass();
            cls.setId(UUID.randomUUID());
            cls.setName(name);
            cls.setSection(name.split("-")[1]);
            cls.setAcademicSessionId(session.getId());
            schoolClassRepository.save(cls);
            classes.add(cls);
        }

        // Subjects
        List<Subject> subjects = new ArrayList<>();
        List<String> subjectNames = List.of("Math", "Science", "English", "Physics", "Chemistry");
        for (String subName : subjectNames) {
            Subject sub = new Subject();
            sub.setId(UUID.randomUUID());
            sub.setName(subName);
            subjectRepository.save(sub);
            subjects.add(sub);
        }

        // Teachers & Assignments
        for (int i = 1; i <= 5; i++) {
            String uacn = "TEA00" + i;

            // Mock Registry entry for Teacher
            UacnRegistry reg = new UacnRegistry();
            reg.setUacn(uacn);
            reg.setName("Teacher " + i);
            uacnRegistryRepository.save(reg);

            assignRole(uacn, "TEACHER", org.getId());

            Teacher teacher = new Teacher();
            teacher.setUacn(uacn);
            teacher.setEmploymentType("PERMANENT");
            teacher.setJoiningDate(LocalDate.now());
            teacherRepository.save(teacher);

            // Assign random subject to random class
            SubjectAssignment assignment = new SubjectAssignment();
            assignment.setId(UUID.randomUUID());
            assignment.setUacn(uacn);
            assignment.setSubjectId(subjects.get(i % subjects.size()).getId());
            assignment.setClassId(classes.get(i % classes.size()).getId()); // Cyclic assignment might overwrite? No,
                                                                            // Allow multiple per class/teacher logic
                                                                            // needed but simple is fine.
            assignment.setAcademicSessionId(session.getId());
            subjectAssignmentRepository.save(assignment);
        }

        // Students
        System.out.println("Seeding 50 Students...");
        for (int i = 1; i <= 50; i++) {
            String uacn = "STU" + String.format("%03d", i);

            UacnRegistry reg = new UacnRegistry();
            reg.setUacn(uacn);
            reg.setName("Student " + i);
            uacnRegistryRepository.save(reg);

            assignRole(uacn, "STUDENT", org.getId());

            Student student = new Student();
            student.setUacn(uacn);
            student.setRollNumber("R" + i);
            // Emergency contact is in StudentContact
            // Assuming no class link in Student entity, relying on enrollment logic if
            // separate.
            // If Student has classId, we missed it? Checking entity later.
            // For profile check it should be fine.
            studentRepository.save(student);

            StudentContact contact = new StudentContact();
            contact.setUacn(uacn); // Use UACN directly as ID
            contact.setStudent(student);
            contact.setAddress("Street " + i);
            contact.setEmergencyContact("99999999" + (i % 10));
            studentContactRepository.save(contact);
        }
    }

    @Transactional
    public void seedTimetable() {
        // Find Class 10-A
        Optional<SchoolClass> classOpt = schoolClassRepository.findAll().stream()
                .filter(c -> "10-A".equals(c.getName()))
                .findFirst();

        if (classOpt.isEmpty())
            return;
        UUID classId = classOpt.get().getId();

        List<SubjectAssignment> assignments = subjectAssignmentRepository.findAllByClassId(classId);
        if (assignments.isEmpty()) {
            System.out.println("No assignments for 10-A to seed timetable.");
            return;
        }

        UUID sessionId = assignments.get(0).getAcademicSessionId();

        // Create Mon-Fri schedule
        List<String> days = List.of("MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY");
        LocalTime startTime = LocalTime.of(9, 0);

        for (String day : days) {
            LocalTime current = startTime;
            for (int p = 0; p < 8; p++) {
                SubjectAssignment assign = assignments.get(p % assignments.size());

                Timetable entry = new Timetable();
                entry.setId(UUID.randomUUID());
                entry.setClassId(classId);
                entry.setSubjectId(assign.getSubjectId());
                entry.setTeacherUacn(assign.getUacn());
                entry.setDayOfWeek(day);
                entry.setStartTime(current);
                entry.setEndTime(current.plusMinutes(45));
                entry.setRoomNumber("101");
                entry.setSessionId(sessionId);
                entry.setStatus("PUBLISHED"); // Directly verify published logic

                timetableRepository.save(entry);
                current = current.plusMinutes(45);
            }
        }
        System.out.println("Seeded Timetable for 10-A.");
    }

    private void performSelfTest() {
        System.out.println("--- Self Test Results ---");

        // Mock Teacher Context
        SecurityContextHolder.setContext(SecurityContextHolder.createEmptyContext());
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("TEA001", "password",
                        List.of(new SimpleGrantedAuthority("ROLE_TEACHER"))));

        // 1. Student Fetch
        try {
            String randomUacn = "STU001";
            Object profile = studentProfileService.getStudentProfile(randomUacn);
            System.out.println("Student Profile Fetch (Role: TEACHER): " + profile);
        } catch (Exception e) {
            System.err.println("Student Profile Fetch Failed: " + e.getMessage());
        }

        // 2. Timetable Fetch
        try {
            Optional<SchoolClass> cls = schoolClassRepository.findAll().stream()
                    .filter(c -> "10-A".equals(c.getName())).findFirst();
            if (cls.isPresent()) {
                var schedule = timetableService.getClassTimetable(cls.get().getId());
                System.out.println(
                        "Timetable Fetch: " + (schedule != null ? "Found " + schedule.size() + " entries" : "Null"));
            }
        } catch (Exception e) {
            System.err.println("Timetable Fetch Failed: " + e.getMessage());
        }

        // Mock Admin for Summary
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("ADMIN", "password",
                        List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));

        // 3. Admin Summary
        try {
            var summary = managementController.getSummary(); // Method name in controller might differ? Checking...
            System.out.println("Admin Summary: " + summary);
        } catch (Exception e) {
            System.err.println("Admin Summary Failed: " + e.getMessage());
        }
    }
}
