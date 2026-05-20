package com.att.tdp.issueflow.service;

import com.att.tdp.issueflow.entity.Ticket;
import com.att.tdp.issueflow.entity.TicketStatus;
import com.att.tdp.issueflow.entity.User;
import com.att.tdp.issueflow.entity.UserRole;
import com.att.tdp.issueflow.repository.TicketRepository;
import com.att.tdp.issueflow.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Optional;

/**
 * Implements the auto-assignment strategy: when a ticket is created without an
 * explicit assignee, find the DEVELOPER user with the fewest currently open tickets
 * and assign them automatically.
 *
 * <h3>Algorithm</h3>
 * <ol>
 *   <li>Fetch all users with role {@code DEVELOPER}.</li>
 *   <li>For each developer, count their open (non-DONE, non-CANCELLED, non-deleted) tickets.</li>
 *   <li>Return the developer with the minimum count.</li>
 *   <li>Tie-break: choose the developer with the lowest {@code id} for determinism.</li>
 *   <li>If no developers exist, return {@link Optional#empty()} — the ticket will be
 *       created with {@code assigneeId = null}.</li>
 * </ol>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AutoAssignmentService {

    private final UserRepository   userRepository;
    private final TicketRepository ticketRepository;

    /** Statuses that count as "closed" — excluded from the open-ticket count. */
    private static final List<TicketStatus> CLOSED_STATUSES =
            List.of(TicketStatus.DONE, TicketStatus.CANCELLED);

    /**
     * Returns the least-loaded DEVELOPER in the system, or {@link Optional#empty()}
     * if no DEVELOPERs exist.
     *
     * <p>This method is called during ticket creation whenever {@code assigneeId}
     * is absent from the request body.</p>
     */
    @Transactional
    public Optional<User> findLeastLoadedDeveloper() {
        // Query the DB directly to find the least loaded developer, getting only the top 1 result,
        // and aggressively locking the row to prevent race conditions during concurrent assignment.
        List<User> topDevelopers = userRepository.findLeastLoadedDeveloperWithLock(
                CLOSED_STATUSES, 
                PageRequest.of(0, 1)
        );

        Optional<User> chosen = topDevelopers.isEmpty() ? Optional.empty() : Optional.of(topDevelopers.get(0));

        chosen.ifPresentOrElse(
                dev -> log.debug("Auto-assignment: assigned to developer '{}' (id={})", dev.getUsername(), dev.getId()),
                () -> log.debug("Auto-assignment: no DEVELOPER users found — ticket will remain unassigned")
        );

        return chosen;
    }
}
