package com.att.tdp.issueflow.entity;

/**
 * The kind of domain object that an {@link AuditLog} entry is associated with.
 */
public enum EntityType {
    TICKET,
    PROJECT,
    COMMENT,
    USER,
    ATTACHMENT,
    DEPENDENCY
}
