package com.att.tdp.issueflow.service;

import com.att.tdp.issueflow.dto.request.AddDependencyRequest;
import com.att.tdp.issueflow.entity.Dependency;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class DependencyServiceTest {

    @Mock
    private DependencyRepository dependencyRepository;

    @Mock
    private TicketService ticketService;

    @InjectMocks
    private DependencyService dependencyService;

    @Test
    void addDependency_SelfReference_ThrowsConflictException() {
        AddDependencyRequest request = new AddDependencyRequest();
        request.setBlockedByTicketId(1L);

        assertThrows(ConflictException.class, () -> dependencyService.addDependency(1L, request));
    }

    @Test
    void addDependency_DuplicateEdge_ThrowsConflictException() {
        AddDependencyRequest request = new AddDependencyRequest();
        request.setBlockedByTicketId(2L);

        Ticket ticket = Ticket.builder().id(1L).build();
        Ticket blocker = Ticket.builder().id(2L).build();

        when(ticketService.getActiveTicketEntity(1L)).thenReturn(ticket);
        when(ticketService.getActiveTicketEntity(2L)).thenReturn(blocker);
        when(dependencyRepository.existsByTicketIdAndBlockerId(1L, 2L)).thenReturn(true);

        assertThrows(ConflictException.class, () -> dependencyService.addDependency(1L, request));
    }

    @Test
    void addDependency_CreatesCycle_ThrowsConflictException() {
        // Graph before addition: 2 -> 3 -> 1
        // We are trying to add: 1 -> 2
        // This creates a cycle: 1 -> 2 -> 3 -> 1
        
        AddDependencyRequest request = new AddDependencyRequest();
        request.setBlockedByTicketId(2L); // 1 is blocked by 2

        Ticket ticket = Ticket.builder().id(1L).build();
        Ticket blocker = Ticket.builder().id(2L).build();

        when(ticketService.getActiveTicketEntity(1L)).thenReturn(ticket);
        when(ticketService.getActiveTicketEntity(2L)).thenReturn(blocker);
        when(dependencyRepository.existsByTicketIdAndBlockerId(1L, 2L)).thenReturn(false);

        // Simulate the DFS traversal:
        // We start searching for 1L starting from 2L (the blocker).
        // 2L is blocked by [3L]
        when(dependencyRepository.findBlockerIdsByTicketId(2L)).thenReturn(List.of(3L));
        // 3L is blocked by [1L]
        when(dependencyRepository.findBlockerIdsByTicketId(3L)).thenReturn(List.of(1L));

        ConflictException ex = assertThrows(ConflictException.class, () -> dependencyService.addDependency(1L, request));
        assert(ex.getMessage().contains("cycle"));
    }

    @Test
    void addDependency_ValidGraph_SavesSuccessfully() {
        // Graph before addition: 2 -> 3
        // We are trying to add: 1 -> 2
        // Safe, no cycle.
        
        AddDependencyRequest request = new AddDependencyRequest();
        request.setBlockedByTicketId(2L);

        Ticket ticket = Ticket.builder().id(1L).build();
        Ticket blocker = Ticket.builder().id(2L).build();

        when(ticketService.getActiveTicketEntity(1L)).thenReturn(ticket);
        when(ticketService.getActiveTicketEntity(2L)).thenReturn(blocker);
        when(dependencyRepository.existsByTicketIdAndBlockerId(1L, 2L)).thenReturn(false);

        when(dependencyRepository.findBlockerIdsByTicketId(2L)).thenReturn(List.of(3L));
        when(dependencyRepository.findBlockerIdsByTicketId(3L)).thenReturn(List.of()); // No further blockers

        Dependency savedDep = Dependency.builder().id(100L).ticket(ticket).blocker(blocker).build();
        when(dependencyRepository.save(any(Dependency.class))).thenReturn(savedDep);

        var response = dependencyService.addDependency(1L, request);
        assert(response.getId().equals(100L));
    }
}
