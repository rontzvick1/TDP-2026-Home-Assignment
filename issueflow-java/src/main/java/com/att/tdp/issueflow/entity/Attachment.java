package com.att.tdp.issueflow.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

/**
 * A file uploaded and attached to a {@link Ticket}.
 *
 * <p>Maps to the {@code attachments} table. The binary file content is stored
 * directly in the {@code data} column as a PostgreSQL {@code bytea} value.
 * The {@code data} field is never included in API responses — only
 * {@code id}, {@code ticketId}, {@code filename}, and {@code contentType} are returned.</p>
 */
@Entity
@Table(name = "attachments")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Attachment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** The ticket this attachment belongs to. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ticket_id", nullable = false)
    private Ticket ticket;

    @Column(nullable = false, length = 500)
    private String filename;

    @Column(name = "content_type", nullable = false, length = 255)
    private String contentType;

    /**
     * Raw binary content of the uploaded file.
     * Mapped to {@code bytea} in PostgreSQL via {@code @Lob}.
     * Must never be serialised in any response DTO.
     */
    @Lob
    @Column(nullable = false)
    private byte[] data;

    @CreationTimestamp
    @Column(name = "uploaded_at", nullable = false, updatable = false)
    private Instant uploadedAt;
}
