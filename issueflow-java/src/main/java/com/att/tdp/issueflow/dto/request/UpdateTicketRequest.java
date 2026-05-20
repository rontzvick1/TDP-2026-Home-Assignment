package com.att.tdp.issueflow.dto.request;

import com.att.tdp.issueflow.entity.TicketPriority;
import com.att.tdp.issueflow.entity.TicketStatus;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

/**
 * Request body for partially updating a ticket (PATCH semantics).
 *
 * <p>All fields are optional. Only non-null fields will be applied to the entity.
 * To explicitly unset the assignee or dueDate, see notes in {@code TicketService}.</p>
 */
@Getter
@NoArgsConstructor
public class UpdateTicketRequest {

    @Size(max = 500, message = "Title must be at most 500 characters")
    private String title;

    private String description;

    /** If non-null, replaces the current status. */
    private TicketStatus status;

    /** If non-null, replaces the current priority. */
    private TicketPriority priority;

    /**
     * If non-null, sets a new assignee by user ID.
     * Use the dedicated unassign flow if needed in future.
     */
    private Long assigneeId;

    /** If non-null, replaces the current due date. */
    private OffsetDateTime dueDate;

    /** If true, unassigns the ticket (assignee becomes null). */
    private Boolean removeAssignee;
}
