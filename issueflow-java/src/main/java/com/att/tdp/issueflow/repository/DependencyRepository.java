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
     * Returns the IDs of all tickets that directly block the given ticket.
     * Used by the DFS cycle-detection algorithm in {@code DependencyService}.
     */
    @Query("SELECT d.blocker.id FROM Dependency d WHERE d.ticket.id = :ticketId")
    List<Long> findBlockerIdsByTicketId(@Param("ticketId") Long ticketId);
}
