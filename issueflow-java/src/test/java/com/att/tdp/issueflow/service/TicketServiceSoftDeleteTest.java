package com.att.tdp.issueflow.service;

import com.att.tdp.issueflow.entity.Project;
import com.att.tdp.issueflow.entity.Ticket;
import com.att.tdp.issueflow.exception.NotFoundException;
import com.att.tdp.issueflow.repository.TicketRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class TicketServiceSoftDeleteTest {

    @Mock
    private TicketRepository ticketRepository;

    @Mock
    private ProjectService projectService;

    @InjectMocks
    private TicketService ticketService;

    @Test
    void getActiveTicketEntity_WhenNotDeleted_ReturnsTicket() {
        Ticket ticket = Ticket.builder().id(1L).deletedAt(null).build();
        when(ticketRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(ticket));

        Ticket result = ticketService.getActiveTicketEntity(1L);
        assertNotNull(result);
    }

    @Test
    void getActiveTicketEntity_WhenDeleted_ThrowsNotFoundException() {
        // FindByIdAndDeletedAtIsNull will return empty because deletedAt is not null in DB
        when(ticketRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> ticketService.getActiveTicketEntity(1L));
    }

    @Test
    void softDeleteTicket_SetsDeletedAtAndSaves() {
        Ticket ticket = Ticket.builder().id(1L).deletedAt(null).build();
        when(ticketRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(ticket));

        ticketService.softDeleteTicket(1L);

        assertNotNull(ticket.getDeletedAt());
        verify(ticketRepository).save(ticket);
    }

    @Test
    void restoreTicket_ClearsDeletedAtAndSaves() {
        Ticket ticket = Ticket.builder().id(1L).deletedAt(Instant.now()).build();
        when(ticketRepository.findById(1L)).thenReturn(Optional.of(ticket));

        ticketService.restoreTicket(1L);

        assertNotNull(ticket);
        assert(ticket.getDeletedAt() == null);
        verify(ticketRepository).save(ticket);
    }
}
