package com.example.controller;

import com.example.service.TenantProvisioningService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/internal")
public class SuperAdminController {

    private final TenantProvisioningService tenantProvisioningService;

    public SuperAdminController(TenantProvisioningService tenantProvisioningService) {
        this.tenantProvisioningService = tenantProvisioningService;
    }

    @PostMapping("/bootstrap-school")
    public ResponseEntity<String> bootstrapSchool(@RequestBody BootstrapRequest request,
            HttpServletRequest httpRequest) {
        // Simple security check for Developer Identity
        String developerKey = httpRequest.getHeader("X-Developer-Key");
        if (!"SUPER-SECRET-DEV-KEY".equals(developerKey)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Access Denied");
        }

        try {
            tenantProvisioningService.createSchool(request.name(), request.aadhaar(), request.principalName());
            return ResponseEntity.ok("School bootstrapped successfully");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to bootstrap school: " + e.getMessage());
        }
    }

    public record BootstrapRequest(String name, String aadhaar, String principalName) {
    }
}
