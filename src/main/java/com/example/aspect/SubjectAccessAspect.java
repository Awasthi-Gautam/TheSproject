package com.example.aspect;

import com.example.repository.SchoolClassRepository;
import com.example.repository.SubjectAssignmentRepository;
import com.example.security.RequiresSubjectAccess;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.example.multitenancy.TenantContext;
import jakarta.persistence.EntityNotFoundException;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Method;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Aspect
@Component
public class SubjectAccessAspect {

    private final SubjectAssignmentRepository subjectAssignmentRepository;
    private final SchoolClassRepository schoolClassRepository;

    private final Cache<String, Boolean> permissionCache;

    public SubjectAccessAspect(SubjectAssignmentRepository subjectAssignmentRepository,
            SchoolClassRepository schoolClassRepository) {
        this.subjectAssignmentRepository = subjectAssignmentRepository;
        this.schoolClassRepository = schoolClassRepository;
        this.permissionCache = Caffeine.newBuilder()
                .expireAfterWrite(5, TimeUnit.MINUTES)
                .maximumSize(1000)
                .build();
    }

    @Before("@annotation(annotation)")
    @Transactional(readOnly = true)
    public void checkAccess(JoinPoint joinPoint, RequiresSubjectAccess annotation) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new AccessDeniedException("User not authenticated");
        }
        String uacn = auth.getName();

        // Admin Bypass
        if (auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))) {
            return;
        }

        UUID classId = getUuidParam(joinPoint, annotation.classIdParam());
        UUID subjectId = getUuidParam(joinPoint, annotation.subjectIdParam());
        String tenantId = TenantContext.getCurrentTenant();

        String cacheKey = tenantId + ":" + uacn + ":" + classId + ":" + subjectId;

        Boolean hasAccess = permissionCache.getIfPresent(cacheKey);
        if (hasAccess == null) {
            hasAccess = checkDbAccess(uacn, classId, subjectId);
            permissionCache.put(cacheKey, hasAccess);
        }

        if (!hasAccess) {
            throw new AccessDeniedException("Access denied for subject assignment.");
        }
    }

    private boolean checkDbAccess(String uacn, UUID classId, UUID subjectId) {
        // 1. Check Class Teacher
        // Assuming we are in tenant context, so SchoolClass is tenant-specific
        boolean isClassTeacher = schoolClassRepository.findById(classId)
                .map(sc -> uacn.equals(sc.getClassTeacherUacn()))
                .orElse(false);

        if (isClassTeacher)
            return true;

        // 2. Check Subject Assignment
        return subjectAssignmentRepository.existsByUacnAndClassIdAndSubjectId(uacn, classId, subjectId);
    }

    private UUID getUuidParam(JoinPoint joinPoint, String paramName) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String[] paramNames = signature.getParameterNames();
        Object[] args = joinPoint.getArgs();

        for (int i = 0; i < paramNames.length; i++) {
            if (paramNames[i].equals(paramName)) {
                if (args[i] instanceof UUID) {
                    return (UUID) args[i];
                } else if (args[i] instanceof String) {
                    return UUID.fromString((String) args[i]);
                }
            }
        }
        throw new IllegalArgumentException("Parameter " + paramName + " not found or not UUID/String");
    }
}
