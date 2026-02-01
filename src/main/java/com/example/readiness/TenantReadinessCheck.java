package com.example.readiness;

import com.example.domain.Organization;
import com.example.repository.OrganizationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Component
public class TenantReadinessCheck implements ApplicationRunner {

    private static final Logger logger = LoggerFactory.getLogger(TenantReadinessCheck.class);
    private final OrganizationRepository organizationRepository;
    private final DataSource dataSource;

    public TenantReadinessCheck(OrganizationRepository organizationRepository, DataSource dataSource) {
        this.organizationRepository = organizationRepository;
        this.dataSource = dataSource;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        logger.info("Starting Tenant Readiness Check...");

        List<Organization> organizations = organizationRepository.findAll();

        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            for (Organization org : organizations) {
                executor.submit(() -> checkTenant(org));
            }
            executor.shutdown();
            executor.awaitTermination(5, TimeUnit.MINUTES);
        }

        logger.info("Tenant Readiness Check Completed.");
    }

    private void checkTenant(Organization org) {
        String schema = org.getSchemaName();
        try {
            JdbcTemplate template = new JdbcTemplate(dataSource);
            template.execute("SET search_path TO " + schema);
            // Simple validation: check if students table exists
            Integer count = template.queryForObject(
                    "SELECT count(*) FROM information_schema.tables WHERE table_schema = ? AND table_name = 'students'",
                    Integer.class, schema);

            if (count != null && count > 0) {
                logger.info("Tenant [{}]: OK", org.getName());
            } else {
                logger.error("Tenant [{}]: FAILED (Schema invalid)", org.getName());
            }
        } catch (Exception e) {
            logger.error("Tenant [{}]: FAILED (Connection error: {})", org.getName(), e.getMessage());
        }
    }
}
