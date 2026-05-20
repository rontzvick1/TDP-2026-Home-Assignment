package com.att.tdp.issueflow.service;

import com.att.tdp.issueflow.dto.request.CreateTicketRequest;
import com.att.tdp.issueflow.dto.request.UpdateTicketRequest;
import com.att.tdp.issueflow.dto.response.TicketResponse;
import com.att.tdp.issueflow.entity.Project;
import com.att.tdp.issueflow.entity.Ticket;
import com.att.tdp.issueflow.entity.TicketPriority;
import com.att.tdp.issueflow.entity.User;
import com.att.tdp.issueflow.annotation.Auditable;
import com.att.tdp.issueflow.entity.AuditAction;
import com.att.tdp.issueflow.entity.EntityType;
import com.att.tdp.issueflow.exception.NotFoundException;
import com.att.tdp.issueflow.repository.TicketRepository;
import com.att.tdp.issueflow.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Business logic for the Tickets API.
 *
 * <h3>Soft-delete contract</h3>
 * <ul>
 *   <li>All standard queries filter {@code deletedAt IS NULL}.</li>
 *   <li>Soft-delete sets {@code deletedAt = now()}.</li>
 *   <li>Restore clears {@code deletedAt = null} (ADMIN only, enforced at controller level).</li>
 * </ul>
 *
 * <h3>Auto-assignment</h3>
 * When {@code assigneeId} is absent from the create request, {@link AutoAssignmentService}
 * selects the least-loaded DEVELOPER. If no DEVELOPERs exist, the ticket is saved unassigned.
 *
 * <h3>isOverdue</h3>
 * Computed in {@link TicketResponse#fromEntity(Ticket)} — never stored in the DB.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TicketService {

    private final TicketRepository      ticketRepository;
    private final UserRepository        userRepository;
    private final ProjectService        projectService;
    private final AutoAssignmentService autoAssignmentService;

    // -----------------------------------------------------------------------
    // Read operations
    // -----------------------------------------------------------------------

    @Transactional(readOnly = true)
    public List<TicketResponse> getTicketsByProject(Long projectId) {
        // Validate the project exists (throws 404 if soft-deleted or missing)
        projectService.getActiveProjectEntity(projectId);

        return ticketRepository.findActiveByProjectId(projectId).stream()
                .map(TicketResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public TicketResponse getTicketById(Long id) {
        Ticket ticket = getActiveTicketEntity(id);
        return TicketResponse.fromEntity(ticket);
    }

    // -----------------------------------------------------------------------
    // Create
    // -----------------------------------------------------------------------

    @Transactional
    @Auditable(action = AuditAction.CREATE, entityType = EntityType.TICKET)
    public TicketResponse createTicket(CreateTicketRequest request) {
        // Validate project exists and is active
        Project project = projectService.getActiveProjectEntity(request.getProjectId());

        // Resolve assignee: explicit or auto-assign
        User assignee = resolveAssignee(request.getAssigneeId());

        Ticket ticket = Ticket.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .status(request.getStatus())
                .priority(request.getPriority())
                .type(request.getType())
                .project(project)
                .assignee(assignee)
                .dueDate(request.getDueDate())
                .build();

        ticket = ticketRepository.save(ticket);
        log.debug("Created ticket id={} in project id={}", ticket.getId(), project.getId());
        return TicketResponse.fromEntity(ticket);
    }

    // -----------------------------------------------------------------------
    // Update
    // -----------------------------------------------------------------------

    @Transactional
    @Auditable(action = AuditAction.UPDATE, entityType = EntityType.TICKET)
    public TicketResponse updateTicket(Long id, UpdateTicketRequest request) {
        Ticket ticket = getActiveTicketEntity(id);

        // Apply only fields that are explicitly provided (non-null)
        if (request.getTitle() != null && !request.getTitle().isBlank()) {
            ticket.setTitle(request.getTitle());
        }
        if (request.getDescription() != null) {
            ticket.setDescription(request.getDescription());
        }
        if (request.getStatus() != null) {
            ticket.setStatus(request.getStatus());
        }
        if (request.getPriority() != null) {
            ticket.setPriority(request.getPriority());
        }
        if (request.getAssigneeId() != null) {
            User newAssignee = userRepository.findById(request.getAssigneeId())
                    .orElseThrow(() -> new NotFoundException("User", request.getAssigneeId()));
            ticket.setAssignee(newAssignee);
        }
        if (request.getDueDate() != null) {
            ticket.setDueDate(request.getDueDate());
        }

        ticket = ticketRepository.save(ticket);
        return TicketResponse.fromEntity(ticket);
    }

    // -----------------------------------------------------------------------
    // Soft Delete
    // -----------------------------------------------------------------------

    @Transactional
    @Auditable(action = AuditAction.DELETE, entityType = EntityType.TICKET)
    public void softDeleteTicket(Long id) {
        Ticket ticket = getActiveTicketEntity(id);
        ticket.setDeletedAt(Instant.now());
        ticketRepository.save(ticket);
    }

    // -----------------------------------------------------------------------
    // Soft Delete API (ADMIN only — enforced at controller via @PreAuthorize)
    // -----------------------------------------------------------------------

    @Transactional(readOnly = true)
    public List<TicketResponse> getDeletedTickets(Long projectId) {
        // Validate the project itself exists (even if deleted)
        projectService.getActiveProjectEntity(projectId);

        return ticketRepository.findDeletedByProjectId(projectId).stream()
                .map(TicketResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional
    @Auditable(action = AuditAction.UPDATE, entityType = EntityType.TICKET)
    public void restoreTicket(Long id) {
        Ticket ticket = ticketRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Ticket", id));

        if (ticket.getDeletedAt() == null) {
            // Already active — idempotent; nothing to do
            return;
        }

        ticket.setDeletedAt(null);
        ticketRepository.save(ticket);
        log.debug("Restored ticket id={}", id);
    }

    // -----------------------------------------------------------------------
    // Internal helpers
    // -----------------------------------------------------------------------

    /**
     * Loads an active (non-deleted) ticket or throws {@link NotFoundException}.
     * Shared by all operations that require an active ticket.
     */
    public Ticket getActiveTicketEntity(Long id) {
        return ticketRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new NotFoundException("Ticket", id));
    }

    /**
     * Resolves the assignee for a ticket:
     * <ul>
     *   <li>If {@code assigneeId} is non-null → validate the user exists and return them.</li>
     *   <li>If {@code assigneeId} is {@code null} → delegate to {@link AutoAssignmentService}.</li>
     * </ul>
     */
    private User resolveAssignee(Long assigneeId) {
        if (assigneeId != null) {
            return userRepository.findById(assigneeId)
                    .orElseThrow(() -> new NotFoundException("Assignee User", assigneeId));
        }
        // Auto-assign: pick the DEVELOPER with the fewest open tickets
        return autoAssignmentService.findLeastLoadedDeveloper().orElse(null);
    }

    /**
     * Escalate ticket priority (used by the background scheduler).
     * @return the ticket ID for the audit log aspect
     */
    @Transactional
    @Auditable(action = AuditAction.UPDATE, entityType = EntityType.TICKET)
    public TicketResponse escalateTicketPriority(Ticket ticket) {
        TicketPriority nextPriority = switch (ticket.getPriority()) {
            case LOW -> TicketPriority.MEDIUM;
            case MEDIUM -> TicketPriority.HIGH;
            case HIGH, CRITICAL -> TicketPriority.CRITICAL;
        };
        ticket.setPriority(nextPriority);
        ticket = ticketRepository.save(ticket);
        return TicketResponse.fromEntity(ticket);
    }
}
