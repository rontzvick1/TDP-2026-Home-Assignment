package com.att.tdp.issueflow.service;

import com.att.tdp.issueflow.entity.TicketStatus;
import com.att.tdp.issueflow.entity.User;
import com.att.tdp.issueflow.repository.TicketRepository;
import com.att.tdp.issueflow.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Implements the auto-assignment strategy: when a ticket is created without an
 * explicit assignee, find the DEVELOPER user with the fewest currently open tickets
 * and assign them automatically.
 *
 * <h3>Algorithm (single-query, no N+1)</h3>
 * <ol>
 *   <li>Call {@code UserRepository.findLeastLoadedDeveloperWithLock} — a single JPQL query
 *       that orders all DEVELOPERs by open ticket count ASC, then by id ASC for tie-breaking,
 *       and returns only the top 1 result with a PESSIMISTIC_WRITE lock.</li>
 *   <li>Return the first result, or {@link Optional#empty()} if no DEVELOPERs exist.</li>
 * </ol>
 *
 * <p>The PESSIMISTIC_WRITE lock prevents two concurrent ticket-creation requests from
 * both selecting the same developer when their ticket counts are equal.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AutoAssignmentService {

    private final UserRepository   userRepository;
    private final TicketRepository ticketRepository; // kept for potential future use

    /**
     * Statuses that count as "closed" — excluded from the open-ticket count.
     * Package-visible (not private) so unit tests in the same package can reference it.
     */
    static final List<TicketStatus> CLOSED_STATUSES =
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
        // Single DB call: fetches the top-1 developer ordered by open ticket count ASC, id ASC.
        // The PESSIMISTIC_WRITE lock in the repository prevents concurrent double-assignment.
        List<User> results = userRepository.findLeastLoadedDeveloperWithLock(
                CLOSED_STATUSES,
                PageRequest.of(0, 1)
        );

        if (results.isEmpty()) {
            log.debug("Auto-assignment: no DEVELOPER users found — ticket will remain unassigned");
            return Optional.empty();
        }

        User chosen = results.get(0);
        log.debug("Auto-assignment: assigned to developer '{}' (id={})",
                chosen.getUsername(), chosen.getId());
        return Optional.of(chosen);
    }
}
