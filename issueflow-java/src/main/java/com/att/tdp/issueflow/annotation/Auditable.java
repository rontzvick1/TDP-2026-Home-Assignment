package com.att.tdp.issueflow.annotation;

import com.att.tdp.issueflow.entity.AuditAction;
import com.att.tdp.issueflow.entity.EntityType;

import java.lang.annotation.*;

/**
 * Marks a method for automatic audit logging via AOP.
 *
 * <p>When applied to a Service layer method that returns a DTO with an {@code id} field
 * (or an entity with an {@code id} field), the {@code AuditAspect} will automatically
 * record an {@code AuditLog} entry upon successful execution.</p>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Auditable {

    /** What kind of action was performed (CREATE, UPDATE, DELETE). */
    AuditAction action();

    /** The type of entity that was affected. */
    EntityType entityType();
}
