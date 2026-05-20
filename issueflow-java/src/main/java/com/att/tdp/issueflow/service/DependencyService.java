package com.att.tdp.issueflow.service;

import com.att.tdp.issueflow.dto.request.AddDependencyRequest;
import com.att.tdp.issueflow.dto.response.DependencyResponse;
import com.att.tdp.issueflow.entity.Dependency;
import com.att.tdp.issueflow.entity.Ticket;
import com.att.tdp.issueflow.exception.ConflictException;
import com.att.tdp.issueflow.exception.NotFoundException;
import com.att.tdp.issueflow.repository.DependencyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Business logic for ticket dependencies with cycle detection.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DependencyService {

    private final DependencyRepository dependencyRepository;
    private final TicketService        ticketService;

    @Transactional(readOnly = true)
    public List<DependencyResponse> getDependencies(Long ticketId) {
        // Validate ticket exists
        ticketService.getActiveTicketEntity(ticketId);

        return dependencyRepository.findByTicketId(ticketId).stream()
                .map(DependencyResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional
    public DependencyResponse addDependency(Long ticketId, AddDependencyRequest request) {
        Long blockerId = request.getBlockedByTicketId();

        // 1. Self-reference check
        if (ticketId.equals(blockerId)) {
            throw new ConflictException("A ticket cannot block itself");
        }

        // 2. Validate both tickets exist and are active
        Ticket ticket  = ticketService.getActiveTicketEntity(ticketId);
        Ticket blocker = ticketService.getActiveTicketEntity(blockerId);

        // 3. Duplicate check
        if (dependencyRepository.existsByTicketIdAndBlockerId(ticketId, blockerId)) {
            throw new ConflictException("Dependency already exists");
        }

        // 4. DFS Cycle Detection
        // If we add an edge (ticket <- blocker), we must ensure that adding this edge
        // doesn't create a cycle. A cycle occurs if 'blocker' is already (transitively)
        // blocked by 'ticket'.
        // We do this by searching for 'ticketId' starting from 'blockerId'.
        if (hasPath(blockerId, ticketId, new HashSet<>())) {
            throw new ConflictException("Adding this dependency would create a cycle");
        }

        // 5. Save the dependency
        Dependency dependency = Dependency.builder()
                .ticket(ticket)
                .blocker(blocker)
                .build();

        dependency = dependencyRepository.save(dependency);
        return DependencyResponse.fromEntity(dependency);
    }

    @Transactional
    public void removeDependency(Long ticketId, Long blockerId) {
        Dependency dependency = dependencyRepository.findByTicketIdAndBlockerId(ticketId, blockerId)
                .orElseThrow(() -> new NotFoundException("Dependency edge not found"));

        dependencyRepository.delete(dependency);
    }

    /**
     * Performs a Depth-First Search (DFS) to determine if a path exists from {@code startNode}
     * to {@code targetNode} in the dependency graph.
     *
     * <p>The graph is defined by edges where {@code ticket.id} points to its {@code blocker.id}.
     * That means if A is blocked by B, there is an edge A -> B.</p>
     *
     * @param currentNode the current ticket ID being visited
     * @param targetNode  the ticket ID we are searching for
     * @param visited     set of already-visited node IDs to prevent infinite loops in broken graphs
     * @return true if a path exists, false otherwise
     */
    private boolean hasPath(Long currentNode, Long targetNode, Set<Long> visited) {
        // Base case: we found the target node
        if (currentNode.equals(targetNode)) {
            return true;
        }

        // Mark current node as visited
        visited.add(currentNode);

        // Get all direct blockers of the current node
        List<Long> blockerIds = dependencyRepository.findBlockerIdsByTicketId(currentNode);

        // Recursively check all unvisited blockers
        for (Long blockerId : blockerIds) {
            if (!visited.contains(blockerId)) {
                if (hasPath(blockerId, targetNode, visited)) {
                    return true;
                }
            }
        }

        // No path found in this branch
        return false;
    }
}
