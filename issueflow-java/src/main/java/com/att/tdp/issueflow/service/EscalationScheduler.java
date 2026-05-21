package com.att.tdp.issueflow.service;

import com.att.tdp.issueflow.entity.Ticket;
import com.att.tdp.issueflow.entity.TicketPriority;
import com.att.tdp.issueflow.entity.TicketStatus;
import com.att.tdp.issueflow.repository.TicketRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Background job that escalates overdue tickets.
 *
 * <h3>Design: Keyset Pagination to prevent OOM</h3>
 * <p>Instead of loading all candidates into memory at once, we process them in chunks of 100
 * using keyset (cursor-based) pagination on the ticket ID. Each iteration starts from the
 * ID after the last one processed, guaranteeing we never hold more than 100 records in memory.</p>
 *
 * <h3>Design: No @Transactional on the scheduler method</h3>
 * <p>We intentionally do NOT make this method transactional. Each individual escalation call
 * ({@code ticketService.escalateTicketPriority}) runs in its own REQUIRES_NEW transaction,
 * so a single failure does not roll back the entire batch. The scheduler is just an orchestrator.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EscalationScheduler {

    private static final int PAGE_SIZE = 100;

    private final TicketRepository ticketRepository;
    private final TicketService    ticketService;

    /**
     * Runs every 5 minutes (300,000 milliseconds).
     * Scans for tickets whose due date is in the past, that are not closed,
     * and whose priority is not already CRITICAL, and escalates them.
     */
    @Scheduled(fixedRate = 300_000)
    public void escalateOverdueTickets() {
        log.info("Running overdue ticket escalation check...");

        List<TicketStatus> closedStatuses = List.of(TicketStatus.DONE, TicketStatus.CANCELLED);
        OffsetDateTime now = OffsetDateTime.now();

        int totalEscalated = 0;
        int totalFailed    = 0;

        // Start cursor at 0; each page begins at id > lastId
        long lastId = 0L;

        while (true) {
            // Fetch the next chunk of candidates using keyset pagination
            List<Ticket> chunk = ticketRepository.findOverdueEscalationCandidates(
                    lastId,
                    now,
                    closedStatuses,
                    TicketPriority.CRITICAL,
                    PageRequest.of(0, PAGE_SIZE)  // always page 0 — cursor does the shifting
            );

            // No more candidates; we are done
            if (chunk.isEmpty()) {
                break;
            }

            for (Ticket ticket : chunk) {
                try {
                    // Each call runs inside its own REQUIRES_NEW transaction.
                    // The @Auditable aspect defaults the actor to 'SYSTEM'
                    // when no SecurityContext is present (background thread).
                    ticketService.escalateTicketPriority(ticket);
                    totalEscalated++;
                    log.debug("Escalated ticket id={}", ticket.getId());
                } catch (Exception e) {
                    totalFailed++;
                    log.error("Failed to escalate ticket id={}: {}", ticket.getId(), e.getMessage());
                }

                // Advance the cursor to the last processed ID in this chunk
                lastId = ticket.getId();
            }

            // If we received fewer records than a full page, there are no more pages
            if (chunk.size() < PAGE_SIZE) {
                break;
            }
        }

        if (totalEscalated > 0 || totalFailed > 0) {
            log.info("Escalation complete. Escalated={}, Failed={}", totalEscalated, totalFailed);
        } else {
            log.debug("No tickets required escalation.");
        }
    }
}
