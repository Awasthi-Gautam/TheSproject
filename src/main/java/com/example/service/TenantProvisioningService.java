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
import java.time.LocalDate;
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
        createSchoolWithSchema(name, schemaName, principalAadhaar, principalName);
    }

    @Transactional
    public void createSchoolWithSchema(String name, String schemaName, String principalAadhaar, String principalName) {
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
        membership.setId(UUID.randomUUID());
        membership.setMemberUacn(principalUacn);
        membership.setOrgId(org.getId());
        membership.setRole("ADMIN"); // Granting Admin access
        membership.setJoinedAt(LocalDate.now());
        orgMembershipRepository.save(membership);
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
