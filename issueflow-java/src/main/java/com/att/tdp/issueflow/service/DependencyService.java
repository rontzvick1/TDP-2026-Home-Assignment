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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Business logic for ticket dependencies with cycle detection.
 *
 * <h3>Design: In-Memory DFS with a pre-fetched adjacency list</h3>
 * <p>The old implementation called the database on every DFS hop
 * ({@code findBlockerIdsByTicketId(currentNode)}). In a graph with N edges, that
 * produced N+1 queries per cycle check — a severe N+1 problem.</p>
 *
 * <p>The new implementation:
 * <ol>
 *   <li>Fetches <em>all</em> active dependency edges for the ticket's project in a single query.</li>
 *   <li>Builds an in-memory adjacency list: {@code Map<ticketId, List<blockerIds>>}.</li>
 *   <li>Runs the DFS entirely in memory, touching the DB only once regardless of graph depth.</li>
 * </ol>
 * Edges involving soft-deleted tickets are automatically excluded by the repository query.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DependencyService {

    private final DependencyRepository dependencyRepository;
    private final TicketService        ticketService;

    @Transactional(readOnly = true)
    public List<DependencyResponse> getDependencies(Long ticketId) {
        // Validate ticket exists and is active
        ticketService.getActiveTicketEntity(ticketId);

        List<Dependency> edges = dependencyRepository.findByTicketId(ticketId);
        List<DependencyResponse> responses = new ArrayList<>();
        for (Dependency edge : edges) {
            responses.add(DependencyResponse.fromEntity(edge));
        }
        return responses;
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

        // 3. Duplicate edge check
        if (dependencyRepository.existsByTicketIdAndBlockerId(ticketId, blockerId)) {
            throw new ConflictException("Dependency already exists");
        }

        // 4. In-Memory DFS Cycle Detection
        //    Fetch all active edges for this project in a single DB round-trip,
        //    then build the adjacency list and run DFS entirely in memory.
        Long projectId = ticket.getProject().getId();
        List<Dependency> allProjectEdges = dependencyRepository.findActiveByProjectId(projectId);
        Map<Long, List<Long>> adjacencyList = buildAdjacencyList(allProjectEdges);

        // Simulate adding the new edge into the in-memory graph before checking.
        // If we add ticket <- blocker, that means from 'blocker' we can reach 'ticket'.
        // A cycle exists if 'ticketId' is already reachable from 'blockerId' (i.e., the graph
        // already has a path blockerId -> ... -> ticketId).
        if (hasPath(blockerId, ticketId, adjacencyList, new HashSet<>())) {
            throw new ConflictException("Adding this dependency would create a cycle");
        }

        // 5. Save the new dependency edge
        Dependency dependency = Dependency.builder()
                .ticket(ticket)
                .blocker(blocker)
                .build();

        dependency = dependencyRepository.save(dependency);
        log.debug("Created dependency: ticket id={} is blocked by id={}", ticketId, blockerId);
        return DependencyResponse.fromEntity(dependency);
    }

    @Transactional
    public void removeDependency(Long ticketId, Long blockerId) {
        Dependency dependency = dependencyRepository.findByTicketIdAndBlockerId(ticketId, blockerId)
                .orElseThrow(() -> new NotFoundException("Dependency edge not found"));

        dependencyRepository.delete(dependency);
        log.debug("Removed dependency: ticket id={} no longer blocked by id={}", ticketId, blockerId);
    }

    // -----------------------------------------------------------------------
    // Private helpers
    // -----------------------------------------------------------------------

    /**
     * Builds an adjacency list from a flat list of dependency edges.
     *
     * <p>Each entry maps a ticket ID to all of its direct blocker IDs.
     * For example, if A is blocked by B and B is blocked by C, the map will be:
     * {@code {A: [B], B: [C]}}.</p>
     *
     * @param edges all dependency edges to include
     * @return a map of ticketId → list of blockerIds
     */
    private Map<Long, List<Long>> buildAdjacencyList(List<Dependency> edges) {
        Map<Long, List<Long>> adjacencyList = new HashMap<>();

        for (Dependency edge : edges) {
            Long fromTicket = edge.getTicket().getId();
            Long toBlocker  = edge.getBlocker().getId();

            // Get or create the list of blockers for this ticket
            List<Long> blockers = adjacencyList.get(fromTicket);
            if (blockers == null) {
                blockers = new ArrayList<>();
                adjacencyList.put(fromTicket, blockers);
            }
            blockers.add(toBlocker);
        }

        return adjacencyList;
    }

    /**
     * Performs a Depth-First Search (DFS) entirely in-memory to determine if a path exists
     * from {@code currentNode} to {@code targetNode} in the dependency graph.
     *
     * <p>The graph follows the direction of the blocker relationship:
     * an edge from {@code ticket} → {@code blocker} means "this ticket is blocked by that one".
     * So to find a cycle when adding (ticket ← blocker), we check whether a path already
     * exists from {@code blocker} to {@code ticket}.</p>
     *
     * @param currentNode   the current ticket ID being explored
     * @param targetNode    the ticket ID we are searching for
     * @param adjacencyList the in-memory graph (ticketId → list of blockerIds)
     * @param visited       set of already-visited node IDs to prevent revisiting nodes
     * @return {@code true} if a path from {@code currentNode} to {@code targetNode} exists
     */
    private boolean hasPath(Long currentNode, Long targetNode,
                            Map<Long, List<Long>> adjacencyList, Set<Long> visited) {
        // Base case: we have reached the target — a path exists
        if (currentNode.equals(targetNode)) {
            return true;
        }

        // Mark this node as visited to avoid revisiting in this traversal
        visited.add(currentNode);

        // Get all direct blockers of the current node from the in-memory map
        List<Long> blockers = adjacencyList.get(currentNode);
        if (blockers == null) {
            // No outgoing edges from this node — dead end
            return false;
        }

        // Recursively explore each unvisited blocker
        for (Long nextNode : blockers) {
            if (!visited.contains(nextNode)) {
                if (hasPath(nextNode, targetNode, adjacencyList, visited)) {
                    return true;
                }
            }
        }

        // No path found from this branch
        return false;
    }
}
