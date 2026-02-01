package com.example.service;

import com.example.domain.OrgMembership;
import com.example.domain.Organization;
import com.example.repository.OrgMembershipRepository;
import com.example.repository.OrganizationRepository;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.FileCopyUtils;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Service
public class TenantProvisioningService {

    private final JdbcTemplate jdbcTemplate;
    private final UacnService uacnService;
    private final OrganizationRepository organizationRepository;
    private final OrgMembershipRepository orgMembershipRepository;

    public TenantProvisioningService(JdbcTemplate jdbcTemplate, UacnService uacnService,
            OrganizationRepository organizationRepository,
            OrgMembershipRepository orgMembershipRepository) {
        this.jdbcTemplate = jdbcTemplate;
        this.uacnService = uacnService;
        this.organizationRepository = organizationRepository;
        this.orgMembershipRepository = orgMembershipRepository;
    }

    @Transactional
    public void createSchool(String name, String principalAadhaar, String principalName) {
        // 1. Generate Schema Name
        String schemaName = "sch_" + name.toLowerCase().replaceAll("[^a-z0-9]", "") + "_"
                + UUID.randomUUID().toString().substring(0, 4);

        // 2. Create Schema
        jdbcTemplate.execute("CREATE SCHEMA " + schemaName);

        // 3. Execute DDL
        executeSchemaScript(schemaName);

        // 4. Create Organization Record
        Organization org = new Organization();
        org.setId(UUID.randomUUID());
        org.setName(name);
        org.setSchemaName(schemaName);
        organizationRepository.save(org);

        // 5. Generate Principal UACN
        String principalUacn = uacnService.generateUacn(principalAadhaar);

        // 6. Create Membership (Principal as ADMIN)
        OrgMembership membership = new OrgMembership();
        membership.setUacn(principalUacn);
        membership.setOrgId(org.getId());
        membership.setRole("ADMIN"); // Granting Admin access
        orgMembershipRepository.save(membership);

        // Also add Principal as Teacher in the tenant schema?
        // Usually yes, but the prompt says: "Insert records into public.organizations
        // and public.org_memberships".
        // It doesn't explicitly say insert into tenant's `teachers` table, but for a
        // bootstrap it's often needed.
        // However, UACN Registry is public. So Auth will work.
        // To be a "Teacher" in the system, they might need a record in `teachers` table
        // of the tenant if we have FK checks.
        // Our `SubjectAssignment` uses UACN string, not FK to `teachers` table (in Java
        // entity, but SQL has FK).
        // SQL has: FOREIGN KEY (teacher_uacn) REFERENCES teachers(uacn) in `timetable`.
        // So if we schedule them, they need to be in `teachers`.
        // But for "SubjectAssignment", the SQL I wrote doesn't enforce FK to `teachers`
        // table (commented out or not present).
        // Let's stick to the prompt requirements: "Insert records into
        // public.organizations and public.org_memberships".
    }

    private void executeSchemaScript(String schemaName) {
        try {
            Resource resource = new ClassPathResource("tenant_template.sql");
            String sql = FileCopyUtils
                    .copyToString(new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8));

            // Set search path for this transaction/session to the new schema so tables are
            // created there
            jdbcTemplate.execute("SET search_path TO " + schemaName);
            jdbcTemplate.execute(sql);
            jdbcTemplate.execute("SET search_path TO public"); // Reset

        } catch (Exception e) {
            throw new RuntimeException("Failed to execute tenant DDL", e);
        }
    }
}
