package com.example.controller;

import com.example.dto.OnboardingRequest;
import com.example.service.UacnService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class StudentOnboardingController {

    private final UacnService uacnService;

    public StudentOnboardingController(UacnService uacnService) {
        this.uacnService = uacnService;
    }

    @PostMapping("/onboard")
    public ResponseEntity<?> onboardStudent(@RequestBody OnboardingRequest request,
            @RequestHeader(value = "X-Tenant-ID", required = true) String tenantId) {
        // The TenantIdentifierResolver should have already picked up the tenantId,
        // but we include it in the signature to enforce it in Swagger/API contract.
        // Actually, the resolver works on the request context, so it's already set for
        // the transaction.

        try {
            String uacn = uacnService.onboardStudent(request.getAadhaar(), request.getName());
            return ResponseEntity.ok(Map.of("message", "Student onboarded successfully", "uacn", uacn));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }
}
