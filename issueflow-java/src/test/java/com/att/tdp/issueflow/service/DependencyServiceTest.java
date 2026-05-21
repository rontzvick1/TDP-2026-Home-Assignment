package com.att.tdp.issueflow.service;

import com.att.tdp.issueflow.dto.request.AddDependencyRequest;
import com.att.tdp.issueflow.entity.Dependency;
import com.att.tdp.issueflow.entity.Project;
import com.att.tdp.issueflow.entity.Ticket;
import com.att.tdp.issueflow.exception.ConflictException;
import com.att.tdp.issueflow.repository.DependencyRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link DependencyService}.
 *
 * <p>DependencyService now fetches all active project edges in a single query
 * ({@code findActiveByProjectId}) and runs DFS entirely in memory.
 * Tests stub that bulk-fetch query rather than the old per-node query.</p>
 */
@ExtendWith(MockitoExtension.class)
public class DependencyServiceTest {

    @Mock
    private DependencyRepository dependencyRepository;

    @Mock
    private TicketService ticketService;

    @InjectMocks
    private DependencyService dependencyService;

    // Helper: build a Project with a given ID
    private Project project(Long id) {
        return Project.builder().id(id).build();
    }

    // Helper: build an active Ticket with a given ID, belonging to a project
    private Ticket ticket(Long id, Project project) {
        return Ticket.builder().id(id).project(project).build();
    }

    // Helper: build a Dependency edge between two tickets
    private Dependency edge(Ticket from, Ticket blocker) {
        return Dependency.builder().ticket(from).blocker(blocker).build();
    }

    @Test
    void addDependency_SelfReference_ThrowsConflictException() {
        AddDependencyRequest request = new AddDependencyRequest();
        request.setBlockedByTicketId(1L);

        // Self-reference check happens before any repository call
        assertThrows(ConflictException.class,
                () -> dependencyService.addDependency(1L, request));

        verifyNoInteractions(dependencyRepository);
    }

    @Test
    void addDependency_DuplicateEdge_ThrowsConflictException() {
        AddDependencyRequest request = new AddDependencyRequest();
        request.setBlockedByTicketId(2L);

        Project proj    = project(10L);
        Ticket ticket   = ticket(1L, proj);
        Ticket blocker  = ticket(2L, proj);

        when(ticketService.getActiveTicketEntity(1L)).thenReturn(ticket);
        when(ticketService.getActiveTicketEntity(2L)).thenReturn(blocker);
        when(dependencyRepository.existsByTicketIdAndBlockerId(1L, 2L)).thenReturn(true);

        assertThrows(ConflictException.class,
                () -> dependencyService.addDependency(1L, request));
    }

    @Test
    void addDependency_CreatesCycle_ThrowsConflictException() {
        // Existing graph:  2 -> 3 -> 1
        // New edge to add: 1 -> 2  (would create cycle 1 -> 2 -> 3 -> 1)
        AddDependencyRequest request = new AddDependencyRequest();
        request.setBlockedByTicketId(2L); // ticket 1 would be blocked by ticket 2

        Project proj    = project(10L);
        Ticket t1       = ticket(1L, proj);
        Ticket t2       = ticket(2L, proj);
        Ticket t3       = ticket(3L, proj);

        when(ticketService.getActiveTicketEntity(1L)).thenReturn(t1);
        when(ticketService.getActiveTicketEntity(2L)).thenReturn(t2);
        when(dependencyRepository.existsByTicketIdAndBlockerId(1L, 2L)).thenReturn(false);

        // Return the full graph for this project upfront:
        // edge(2 -> 3): ticket 2 is blocked by ticket 3
        // edge(3 -> 1): ticket 3 is blocked by ticket 1
        List<Dependency> projectEdges = List.of(edge(t2, t3), edge(t3, t1));
        when(dependencyRepository.findActiveByProjectId(eq(10L))).thenReturn(projectEdges);

        ConflictException ex = assertThrows(ConflictException.class,
                () -> dependencyService.addDependency(1L, request));
        assert ex.getMessage().contains("cycle");
    }

    @Test
    void addDependency_ValidGraph_SavesSuccessfully() {
        // Existing graph:  2 -> 3
        // New edge to add: 1 -> 2  (safe, no cycle)
        AddDependencyRequest request = new AddDependencyRequest();
        request.setBlockedByTicketId(2L);

        Project proj    = project(10L);
        Ticket t1       = ticket(1L, proj);
        Ticket t2       = ticket(2L, proj);
        Ticket t3       = ticket(3L, proj);

        when(ticketService.getActiveTicketEntity(1L)).thenReturn(t1);
        when(ticketService.getActiveTicketEntity(2L)).thenReturn(t2);
        when(dependencyRepository.existsByTicketIdAndBlockerId(1L, 2L)).thenReturn(false);

        // Only one edge in the project: ticket 2 is blocked by ticket 3
        List<Dependency> projectEdges = List.of(edge(t2, t3));
        when(dependencyRepository.findActiveByProjectId(eq(10L))).thenReturn(projectEdges);

        Dependency savedDep = Dependency.builder().id(100L).ticket(t1).blocker(t2).build();
        when(dependencyRepository.save(any(Dependency.class))).thenReturn(savedDep);

        var response = dependencyService.addDependency(1L, request);
        assert response.getId().equals(100L);
    }
}
