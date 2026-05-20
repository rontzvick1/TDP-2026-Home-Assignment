package com.att.tdp.issueflow.aspect;

import com.att.tdp.issueflow.annotation.Auditable;
import com.att.tdp.issueflow.entity.ActorType;
import com.att.tdp.issueflow.entity.AuditLog;
import com.att.tdp.issueflow.entity.User;
import com.att.tdp.issueflow.repository.AuditLogRepository;
import com.att.tdp.issueflow.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

/**
 * Aspect that automatically intercepts methods annotated with {@link Auditable}
 * and saves an {@link AuditLog} record.
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class AuditAspect {

    private final AuditLogRepository auditLogRepository;
    private final UserRepository     userRepository;

    /**
     * Executes after the intercepted method returns successfully.
     *
     * @param joinPoint the point of execution (the method call)
     * @param auditable the annotation instance
     * @param result    the object returned by the method (can be null for void methods)
     */
    @AfterReturning(value = "@annotation(auditable)", returning = "result")
    public void logAuditActivity(JoinPoint joinPoint, Auditable auditable, Object result) {
        try {
            Long entityId = extractEntityId(joinPoint, result);
            if (entityId == null) {
                log.warn("Could not extract entityId for audit log on method: {}",
                        joinPoint.getSignature().getName());
                return;
            }

            ActorType actorType = ActorType.SYSTEM;
            User performedBy = null;

            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated() && !auth.getPrincipal().equals("anonymousUser")) {
                // We have a real user context
                actorType = ActorType.USER;
                String username = auth.getName();
                performedBy = userRepository.findByUsername(username).orElse(null);
            }

            AuditLog auditLog = AuditLog.of(
                    auditable.action(),
                    auditable.entityType(),
                    entityId,
                    performedBy,
                    actorType
            );

            auditLogRepository.save(auditLog);
            log.debug("Saved audit log: {} {} on ID {} by {}",
                    auditable.action(), auditable.entityType(), entityId, actorType);

        } catch (Exception e) {
            // We swallow exceptions here because audit logging failure should not
            // rollback or break the business transaction that just succeeded.
            log.error("Failed to generate audit log: {}", e.getMessage(), e);
        }
    }

    /**
     * Attempts to resolve the affected entity ID.
     * <ol>
     *   <li>If the method returns an object with a {@code getId()} method, use that.
     *       (Typical for CREATE / UPDATE which return a Response DTO).</li>
     *   <li>If the method returns void (or no ID is found in the result), look for a {@code Long}
     *       argument in the method signature, assuming it's the primary key (e.g. for DELETE).</li>
     * </ol>
     */
    private Long extractEntityId(JoinPoint joinPoint, Object result) {
        // Strategy 1: check return object
        if (result != null) {
            try {
                Method getIdMethod = result.getClass().getMethod("getId");
                Object idValue = getIdMethod.invoke(result);
                if (idValue instanceof Long) {
                    return (Long) idValue;
                }
            } catch (NoSuchMethodException e) {
                // Expected if the result doesn't have an ID (e.g., void)
            } catch (Exception e) {
                log.warn("Error extracting ID from return object", e);
            }
        }

        // Strategy 2: check method arguments (assumes the ID is passed as a Long)
        for (Object arg : joinPoint.getArgs()) {
            if (arg instanceof Long) {
                return (Long) arg;
            }
        }

        return null;
    }
}
