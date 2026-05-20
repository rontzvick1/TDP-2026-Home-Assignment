package com.att.tdp.issueflow.entity;

/**
 * Identifies who (or what) triggered a state-changing action recorded in an {@link AuditLog}.
 * <ul>
 *   <li>{@code USER}   – an authenticated HTTP request from a human user.</li>
 *   <li>{@code SYSTEM} – an automated background process (e.g., the escalation scheduler).</li>
 * </ul>
 */
public enum ActorType {
    USER,
    SYSTEM
}
