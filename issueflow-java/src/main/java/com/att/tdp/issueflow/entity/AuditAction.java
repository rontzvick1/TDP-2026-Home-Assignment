package com.att.tdp.issueflow.entity;

/**
 * The kind of state-changing operation recorded in an {@link AuditLog} entry.
 */
public enum AuditAction {
    CREATE,
    UPDATE,
    DELETE
}
