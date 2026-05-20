package com.att.tdp.issueflow.dto.response;

import com.att.tdp.issueflow.entity.ActorType;
import com.att.tdp.issueflow.entity.AuditAction;
import com.att.tdp.issueflow.entity.AuditLog;
import com.att.tdp.issueflow.entity.EntityType;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

/**
 * Public view of an {@link AuditLog} entry.
 */
@Getter
@Builder
public class AuditLogResponse {

    private final Long id;
    private final AuditAction action;
    private final EntityType entityType;
    private final Long entityId;
    
    /** Username of the person who performed the action. Null if actor is SYSTEM. */
    private final String performedBy;
    
    private final ActorType actor;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'", timezone = "UTC")
    private final Instant timestamp;

    public static AuditLogResponse fromEntity(AuditLog log) {
        return AuditLogResponse.builder()
                .id(log.getId())
                .action(log.getAction())
                .entityType(log.getEntityType())
                .entityId(log.getEntityId())
                .performedBy(log.getPerformedBy() != null ? log.getPerformedBy().getUsername() : null)
                .actor(log.getActor())
                .timestamp(log.getTimestamp())
                .build();
    }
}
