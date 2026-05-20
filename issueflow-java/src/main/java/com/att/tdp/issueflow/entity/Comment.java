package com.att.tdp.issueflow.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

/**
 * A user-written comment on a {@link Ticket}.
 *
 * <p>Maps to the {@code comments} table. The {@code mentionedUsers} collection
 * is persisted via the {@code comment_mentions} join table and is populated at
 * save time by parsing {@code @username} patterns in {@code content}.</p>
 */
@Entity
@Table(name = "comments")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Comment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** The ticket this comment is attached to. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ticket_id", nullable = false)
    private Ticket ticket;

    /** The user who wrote this comment. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id", nullable = false)
    private User author;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    /**
     * Users mentioned via {@code @username} in {@link #content}.
     * Populated by {@code MentionService} at save/update time.
     * Stored in the {@code comment_mentions} join table.
     */
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "comment_mentions",
        joinColumns        = @JoinColumn(name = "comment_id"),
        inverseJoinColumns = @JoinColumn(name = "user_id")
    )
    @Builder.Default
    private Set<User> mentionedUsers = new HashSet<>();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
