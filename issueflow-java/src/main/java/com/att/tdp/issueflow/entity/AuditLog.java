package com.att.tdp.issueflow.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * An immutable record of a state-changing action in the system.
 *
 * <p>Maps to the {@code audit_logs} table. Entries are written automatically
 * by the {@code AuditAspect} and must never be modified or deleted via the API.</p>
 *
 * <ul>
 *   <li>{@code performedBy} is nullable — it is {@code null} when {@code actor = SYSTEM}
 *       (e.g., entries created by the auto-escalation scheduler).</li>
 *   <li>{@code entityId} is the PK of the affected row in its respective table.</li>
 * </ul>
 */
@Entity
@Table(name = "audit_logs")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** What kind of operation was performed. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AuditAction action;

    /** Which type of domain object was affected. */
    @Enumerated(EnumType.STRING)
    @Column(name = "entity_type", nullable = false, length = 30)
    private EntityType entityType;

    /** Primary key of the affected row. */
    @Column(name = "entity_id", nullable = false)
    private Long entityId;

    /**
     * The authenticated user who triggered the action.
     * {@code null} for SYSTEM-initiated actions (e.g., scheduler).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "performed_by")
    private User performedBy;

    /** Whether the action was initiated by a human user or the system. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private ActorType actor;

    @Column(nullable = false)
    private Instant timestamp;

    /** Convenience factory that stamps the current time automatically. */
    public static AuditLog of(AuditAction action, EntityType entityType,
                               Long entityId, User performedBy, ActorType actor) {
        return AuditLog.builder()
                .action(action)
                .entityType(entityType)
                .entityId(entityId)
                .performedBy(performedBy)
                .actor(actor)
                .timestamp(Instant.now())
                .build();
    }
}
