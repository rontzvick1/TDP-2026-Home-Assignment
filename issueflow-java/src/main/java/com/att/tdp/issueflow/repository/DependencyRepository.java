package com.att.tdp.issueflow.repository;

import com.att.tdp.issueflow.entity.Dependency;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Data access layer for {@link Dependency} (ticket blocker) edges.
 */
@Repository
public interface DependencyRepository extends JpaRepository<Dependency, Long> {

    /** Returns all blockers of a given ticket (i.e., tickets that block it). */
    List<Dependency> findByTicketId(Long ticketId);

    /** Checks whether a dependency edge already exists (duplicate guard). */
    boolean existsByTicketIdAndBlockerId(Long ticketId, Long blockerId);

    /** Finds a specific dependency edge for removal. */
    Optional<Dependency> findByTicketIdAndBlockerId(Long ticketId, Long blockerId);

    /**
     * Fetches ALL active (non-deleted) dependency edges for a given project upfront.
     * Used to build an in-memory adjacency list for the DFS cycle check.
     * Filtering by project prevents loading unrelated data and keeps the
     * graph scope tight.
     */
    @Query("""
        SELECT d FROM Dependency d
        WHERE d.ticket.project.id = :projectId
          AND d.ticket.deletedAt IS NULL
          AND d.blocker.deletedAt IS NULL
    """)
    List<Dependency> findActiveByProjectId(@Param("projectId") Long projectId);
}
