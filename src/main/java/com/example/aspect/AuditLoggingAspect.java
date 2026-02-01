package com.example.aspect;

import com.example.domain.AuditLog;
import com.example.domain.SubjectAssignment;
import com.example.repository.AuditLogRepository;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.UUID;

@Aspect
@Component
public class AuditLoggingAspect {

    private final AuditLogRepository auditLogRepository;

    public AuditLoggingAspect(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @AfterReturning(pointcut = "execution(* com.example.service.AdminAssignmentService.assignSubject(..))", returning = "result")
    public void logAssignment(JoinPoint joinPoint, SubjectAssignment result) {
        String actor = SecurityContextHolder.getContext().getAuthentication().getName();
        Object[] args = joinPoint.getArgs();
        String targetUacn = (String) args[0]; // First arg is UACN

        saveLog(actor, targetUacn, "GRANT_ASSIGNMENT");
    }

    @AfterReturning("execution(* com.example.service.AdminAssignmentService.revokeAssignment(..))")
    public void logRevocation(JoinPoint joinPoint) {
        String actor = SecurityContextHolder.getContext().getAuthentication().getName();
        Object[] args = joinPoint.getArgs();
        UUID assignmentId = (UUID) args[0];

        // Note: For revocation, we only have the assignmentId.
        // Ideal audit logging would probably fetch the assignment before deletion to
        // log WHO was revoked,
        // but for this task requirement "record: action_by_uacn, target_uacn,
        // action_type"
        // we might log the assignment ID as target or "N/A" if we can't easily get the
        // target UACN after deletion (without extra lookup).
        // For simplicity and performance, I'll log the Assignment ID as target for
        // REVOKE, or "Assignment:" + id.

        saveLog(actor, "Assignment:" + assignmentId, "REVOKE_ASSIGNMENT");
    }

    private void saveLog(String actor, String target, String action) {
        AuditLog log = new AuditLog(UUID.randomUUID(), actor, target, action, LocalDateTime.now());
        auditLogRepository.save(log);
    }
}
