package com.att.tdp.issueflow.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * A blocker relationship between two {@link Ticket}s.
 *
 * <p>Maps to the {@code ticket_dependencies} table. Each row means
 * "{@link #ticket} is blocked by {@link #blocker}".</p>
 *
 * <p>Constraints enforced at the database level:</p>
 * <ul>
 *   <li>Unique pair {@code (ticket_id, blocked_by)} — no duplicate edges.</li>
 *   <li>No self-reference — enforced in {@code DependencyService} before saving.</li>
 *   <li>No cycles — DFS check performed in {@code DependencyService} before saving.</li>
 * </ul>
 */
@Entity
@Table(
    name = "ticket_dependencies",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uq_ticket_dependency",
            columnNames = {"ticket_id", "blocked_by"}
        )
    }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Dependency {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** The ticket that is being blocked. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ticket_id", nullable = false)
    private Ticket ticket;

    /** The ticket that acts as the blocker. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "blocked_by", nullable = false)
    private Ticket blocker;
}
