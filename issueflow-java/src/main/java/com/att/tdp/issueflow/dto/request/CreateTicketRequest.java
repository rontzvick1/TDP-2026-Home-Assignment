package com.att.tdp.issueflow.dto.request;

import com.att.tdp.issueflow.entity.TicketPriority;
import com.att.tdp.issueflow.entity.TicketStatus;
import com.att.tdp.issueflow.entity.TicketType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;

/**
 * Request body for creating a new ticket.
 */
@Getter
@Setter
@NoArgsConstructor
public class CreateTicketRequest {

    @NotBlank(message = "Title must not be blank")
    @Size(max = 500, message = "Title must be at most 500 characters")
    private String title;

    private String description;

    @NotNull(message = "Status must not be null")
    private TicketStatus status;

    @NotNull(message = "Priority must not be null")
    private TicketPriority priority;

    @NotNull(message = "Type must not be null")
    private TicketType type;

    @NotNull(message = "Project ID must not be null")
    private Long projectId;

    /**
     * Optional. If {@code null}, auto-assignment kicks in:
     * the least-loaded DEVELOPER in the system will be assigned.
     */
    private Long assigneeId;

    /**
     * Optional deadline. If non-null and in the past for a non-closed ticket,
     * {@code isOverdue} will be {@code true} in the response.
     */
    private OffsetDateTime dueDate;
}
