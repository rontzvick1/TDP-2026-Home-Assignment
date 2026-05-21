package com.att.tdp.issueflow.service;

import com.att.tdp.issueflow.entity.Project;
import com.att.tdp.issueflow.entity.Ticket;
import com.att.tdp.issueflow.exception.ConflictException;
import com.att.tdp.issueflow.exception.NotFoundException;
import com.att.tdp.issueflow.repository.TicketRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for soft-delete and restore behaviour in {@link TicketService}.
 */
@ExtendWith(MockitoExtension.class)
public class TicketServiceSoftDeleteTest {

    @Mock
    private TicketRepository ticketRepository;

    @Mock
    private ProjectService projectService;

    @InjectMocks
    private TicketService ticketService;

    // Helper — an active project (deletedAt = null)
    private Project activeProject() {
        return Project.builder().id(10L).deletedAt(null).build();
    }

    // Helper — a soft-deleted project (deletedAt = some instant)
    private Project deletedProject() {
        return Project.builder().id(10L).deletedAt(Instant.now()).build();
    }

    @Test
    void getActiveTicketEntity_WhenNotDeleted_ReturnsTicket() {
        Ticket ticket = Ticket.builder().id(1L).deletedAt(null).build();
        when(ticketRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(ticket));

        Ticket result = ticketService.getActiveTicketEntity(1L);
        assertNotNull(result);
    }

    @Test
    void getActiveTicketEntity_WhenDeleted_ThrowsNotFoundException() {
        when(ticketRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class,
                () -> ticketService.getActiveTicketEntity(1L));
    }

    @Test
    void softDeleteTicket_SetsDeletedAtAndSaves() {
        Ticket ticket = Ticket.builder().id(1L).deletedAt(null).build();
        when(ticketRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(ticket));

        ticketService.softDeleteTicket(1L);

        assertNotNull(ticket.getDeletedAt(), "deletedAt should be set after soft-delete");
        verify(ticketRepository).save(ticket);
    }

    @Test
    void restoreTicket_WhenParentProjectIsActive_ClearsDeletedAtAndSaves() {
        // Ticket is soft-deleted; its parent project is still active
        Ticket ticket = Ticket.builder().id(1L).deletedAt(Instant.now()).project(activeProject()).build();
        when(ticketRepository.findById(1L)).thenReturn(Optional.of(ticket));

        ticketService.restoreTicket(1L);

        assertNull(ticket.getDeletedAt(), "deletedAt should be null after restore");
        verify(ticketRepository).save(ticket);
    }

    @Test
    void restoreTicket_WhenParentProjectIsDeleted_ThrowsConflictException() {
        // Ticket is soft-deleted; its parent project is also soft-deleted → cannot restore
        Ticket ticket = Ticket.builder().id(1L).deletedAt(Instant.now()).project(deletedProject()).build();
        when(ticketRepository.findById(1L)).thenReturn(Optional.of(ticket));

        assertThrows(ConflictException.class,
                () -> ticketService.restoreTicket(1L),
                "Should throw ConflictException when parent project is soft-deleted");
    }
}
