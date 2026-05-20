package com.att.tdp.issueflow.repository;

import com.att.tdp.issueflow.entity.Ticket;
import com.att.tdp.issueflow.entity.TicketPriority;
import com.att.tdp.issueflow.entity.TicketStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Data access layer for {@link Ticket} entities.
 *
 * <p>Soft-delete contract: all "active" queries filter {@code deleted_at IS NULL};
 * "deleted" queries filter {@code deleted_at IS NOT NULL}.</p>
 */
@Repository
public interface TicketRepository extends JpaRepository<Ticket, Long> {

    // -----------------------------------------------------------------------
    // Active ticket queries
    // -----------------------------------------------------------------------

    /** Returns all non-deleted tickets belonging to a project. */
    @Query("SELECT t FROM Ticket t WHERE t.project.id = :projectId AND t.deletedAt IS NULL")
    List<Ticket> findActiveByProjectId(@Param("projectId") Long projectId);

    /** Returns a non-deleted ticket by ID. */
    Optional<Ticket> findByIdAndDeletedAtIsNull(Long id);

    // -----------------------------------------------------------------------
    // Soft-deleted ticket queries (ADMIN)
    // -----------------------------------------------------------------------

    /** Returns all soft-deleted tickets for a project. */
    @Query("SELECT t FROM Ticket t WHERE t.project.id = :projectId AND t.deletedAt IS NOT NULL")
    List<Ticket> findDeletedByProjectId(@Param("projectId") Long projectId);

    // -----------------------------------------------------------------------
    // Auto-assignment: count open tickets per user
    // -----------------------------------------------------------------------

    /**
     * Counts non-deleted, open (not DONE/CANCELLED) tickets assigned to a specific user.
     * Used by {@code AutoAssignmentService} to find the least-loaded DEVELOPER.
     */
    @Query("""
            SELECT COUNT(t) FROM Ticket t
            WHERE t.assignee.id = :userId
              AND t.deletedAt IS NULL
              AND t.status NOT IN :closedStatuses
            """)
    long countOpenTicketsByAssignee(
            @Param("userId") Long userId,
            @Param("closedStatuses") List<TicketStatus> closedStatuses);

    // -----------------------------------------------------------------------
    // Auto-escalation scheduler
    // -----------------------------------------------------------------------

    /**
     * Returns all non-deleted, non-closed tickets whose {@code dueDate} has passed
     * and whose priority has not yet reached CRITICAL.
     * Called by {@code EscalationScheduler} every 5 minutes.
     */
    @Query("""
            SELECT t FROM Ticket t
            WHERE t.deletedAt IS NULL
              AND t.dueDate IS NOT NULL
              AND t.dueDate < :now
              AND t.status NOT IN :closedStatuses
              AND t.priority <> :critical
            """)
    List<Ticket> findOverdueEscalationCandidates(
            @Param("now") OffsetDateTime now,
            @Param("closedStatuses") List<TicketStatus> closedStatuses,
            @Param("critical") TicketPriority critical);

    // -----------------------------------------------------------------------
    // Workload API
    // -----------------------------------------------------------------------

    /**
     * Returns the workload for users in a project based on open tickets.
     */
    @Query("""
            SELECT new com.att.tdp.issueflow.dto.response.WorkloadResponse(u.id, u.username, COUNT(t))
            FROM Ticket t JOIN t.assignee u
            WHERE t.project.id = :projectId
              AND t.deletedAt IS NULL
              AND t.status NOT IN :closedStatuses
            GROUP BY u.id, u.username
            """)
    List<com.att.tdp.issueflow.dto.response.WorkloadResponse> getProjectWorkload(
            @Param("projectId") Long projectId,
            @Param("closedStatuses") List<TicketStatus> closedStatuses);
}
