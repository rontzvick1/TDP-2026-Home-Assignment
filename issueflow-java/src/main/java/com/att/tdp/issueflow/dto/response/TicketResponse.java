package com.att.tdp.issueflow.dto.response;

import com.att.tdp.issueflow.entity.Ticket;
import com.att.tdp.issueflow.entity.TicketPriority;
import com.att.tdp.issueflow.entity.TicketStatus;
import com.att.tdp.issueflow.entity.TicketType;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.time.OffsetDateTime;

/**
 * Public view of a {@link Ticket}.
 *
 * <p>{@code isOverdue} is a computed field — it is NOT stored in the database.
 * It is evaluated at mapping time using the formula:
 * <pre>
 *   dueDate != null
 *     &amp;&amp; dueDate.isBefore(OffsetDateTime.now())
 *     &amp;&amp; status != DONE
 *     &amp;&amp; status != CANCELLED
 * </pre>
 * </p>
 */
@Getter
@Builder
public class TicketResponse {

    private final Long         id;
    private final String       title;
    private final String       description;
    private final TicketStatus status;
    private final TicketPriority priority;
    private final TicketType   type;
    private final Long         projectId;
    private final Long         assigneeId;   // null if unassigned

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ssXXX")
    private final OffsetDateTime dueDate;    // null if no deadline

    /**
     * {@code true} when the ticket's deadline has passed and it is still open.
     * Computed fresh on every response — never persisted.
     */
    private final boolean isOverdue;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'", timezone = "UTC")
    private final Instant createdAt;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'", timezone = "UTC")
    private final Instant updatedAt;

    /**
     * Maps a {@link Ticket} entity to this DTO, computing {@code isOverdue} inline.
     *
     * <p>This is the single authoritative place where {@code isOverdue} is evaluated,
     * ensuring consistency across all endpoints that return ticket data.</p>
     */
    public static TicketResponse fromEntity(Ticket ticket) {
        // ── isOverdue computation ─────────────────────────────────────────
        // A ticket is overdue when:
        //  1. It has a due date
        //  2. That due date is strictly before now
        //  3. The ticket is still in an open state (not DONE or CANCELLED)
        boolean isOverdue = ticket.getDueDate() != null
                && ticket.getDueDate().isBefore(OffsetDateTime.now())
                && ticket.getStatus() != TicketStatus.DONE
                && ticket.getStatus() != TicketStatus.CANCELLED;

        return TicketResponse.builder()
                .id(ticket.getId())
                .title(ticket.getTitle())
                .description(ticket.getDescription())
                .status(ticket.getStatus())
                .priority(ticket.getPriority())
                .type(ticket.getType())
                .projectId(ticket.getProject().getId())
                .assigneeId(ticket.getAssignee() != null ? ticket.getAssignee().getId() : null)
                .dueDate(ticket.getDueDate())
                .isOverdue(isOverdue)
                .createdAt(ticket.getCreatedAt())
                .updatedAt(ticket.getUpdatedAt())
                .build();
    }
}
