package com.att.tdp.issueflow.entity;

/**
 * Roles available to a {@link User} in the system.
 * <ul>
 *   <li>{@code ADMIN}     – full access including soft-delete restore and user management.</li>
 *   <li>{@code DEVELOPER} – can create and update tickets and comments.</li>
 * </ul>
 */
public enum UserRole {
    ADMIN,
    DEVELOPER
}
