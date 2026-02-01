package com.example.multitenancy;

import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Optional;

@Component
public class TenantIdentifierResolver implements CurrentTenantIdentifierResolver {

    private static final String TENANT_HEADER = "X-Tenant-ID";

    @Override
    public String resolveCurrentTenantIdentifier() {
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
        if (requestAttributes instanceof ServletRequestAttributes) {
            String tenantId = ((ServletRequestAttributes) requestAttributes).getRequest().getHeader(TENANT_HEADER);
            if (tenantId != null && !tenantId.isBlank()) {
                return tenantId;
            }
        }
        throw new com.example.exception.TenantNotFoundException(
                "Tenant header " + TENANT_HEADER + " is missing or empty.");
    }

    @Override
    public boolean validateExistingCurrentSessions() {
        return true;
    }
}
