package com.att.tdp.issueflow.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * A top-level container that groups related {@link Ticket}s.
 *
 * <p>Maps to the {@code projects} table. Soft-delete is implemented via
 * {@code deletedAt}: a {@code null} value means the project is active;
 * a non-null value means it has been soft-deleted.</p>
 *
 * <p>All standard queries must filter {@code deleted_at IS NULL}.
 * Only ADMIN users may list or restore soft-deleted projects.</p>
 */
@Entity
@Table(name = "projects")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Project {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    /**
     * The user who owns this project.
     * Stored as a FK ({@code owner_id}) in the database.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    /**
     * Soft-delete timestamp. {@code null} = active. Non-null = deleted.
     * Set by the soft-delete endpoint; cleared by the restore endpoint.
     */
    @Column(name = "deleted_at")
    private Instant deletedAt;
}
