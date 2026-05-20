package com.att.tdp.issueflow.service;

import com.att.tdp.issueflow.entity.Ticket;
import com.att.tdp.issueflow.entity.TicketPriority;
import com.att.tdp.issueflow.entity.TicketStatus;
import com.att.tdp.issueflow.repository.TicketRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Background job that escalates overdue tickets.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EscalationScheduler {

    private final TicketRepository ticketRepository;
    private final TicketService    ticketService;

    /**
     * Runs every 5 minutes (300,000 milliseconds).
     * Scans for tickets whose due date is in the past, that are not closed,
     * and whose priority is not already CRITICAL, and escalates them.
     */
    @Scheduled(fixedRate = 300000)
    @Transactional
    public void escalateOverdueTickets() {
        log.info("Running overdue ticket escalation check...");

        List<TicketStatus> closedStatuses = List.of(TicketStatus.DONE, TicketStatus.CANCELLED);
        List<Ticket> candidates = ticketRepository.findOverdueEscalationCandidates(
                OffsetDateTime.now(),
                closedStatuses,
                TicketPriority.CRITICAL
        );

        int escalatedCount = 0;
        for (Ticket ticket : candidates) {
            try {
                // By calling this Service method, the @Auditable Aspect automatically intercepts it
                // and generates an AuditLog entry. Since it runs in a background thread without
                // a SecurityContext, the Aspect defaults the actor to 'SYSTEM'.
                ticketService.escalateTicketPriority(ticket);
                escalatedCount++;
                log.debug("Escalated ticket ID {} to new priority", ticket.getId());
            } catch (Exception e) {
                log.error("Failed to escalate ticket ID {}: {}", ticket.getId(), e.getMessage());
            }
        }

        if (escalatedCount > 0) {
            log.info("Escalation complete. Escalated {} tickets.", escalatedCount);
        } else {
            log.debug("No tickets required escalation.");
        }
    }
}
