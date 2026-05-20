package com.att.tdp.issueflow.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.time.OffsetDateTime;

/**
 * The core work item (issue) tracked in the system.
 *
 * <p>Maps to the {@code tickets} table. Key behaviours:</p>
 * <ul>
 *   <li><b>Soft-delete</b> – {@code deletedAt} is set on DELETE; cleared on restore.</li>
 *   <li><b>Auto-assignment</b> – {@code assignee} may be {@code null} at creation time;
 *       the service layer fills it with the least-loaded DEVELOPER in the project.</li>
 *   <li><b>isOverdue</b> – NOT a database column. Computed in the service layer
 *       using {@code dueDate &lt; now() &amp;&amp; status NOT IN (DONE, CANCELLED)}.</li>
 *   <li><b>Priority escalation</b> – the background scheduler bumps {@code priority}
 *       one level when {@code dueDate} is exceeded.</li>
 * </ul>
 */
@Entity
@Table(name = "tickets")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Ticket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 500)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TicketStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TicketPriority priority;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TicketType type;

    /** The project this ticket belongs to. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    /**
     * The user assigned to work on this ticket.
     * May be {@code null} if no DEVELOPER exists in the system.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assignee_id")
    private User assignee;

    /**
     * Optional deadline for the ticket.
     * Used by the auto-escalation scheduler and the {@code isOverdue} computation.
     */
    @Column(name = "due_date", columnDefinition = "TIMESTAMP WITH TIME ZONE")
    private OffsetDateTime dueDate;

    /**
     * Soft-delete timestamp. {@code null} = active. Non-null = deleted.
     */
    @Column(name = "deleted_at")
    private Instant deletedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
