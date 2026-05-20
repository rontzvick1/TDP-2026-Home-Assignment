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

import java.util.Comparator;
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
    @Transactional(readOnly = true)
    public Optional<User> findLeastLoadedDeveloper() {
        List<User> developers = userRepository.findAllByRole(UserRole.DEVELOPER);

        if (developers.isEmpty()) {
            log.debug("Auto-assignment: no DEVELOPER users found — ticket will remain unassigned");
            return Optional.empty();
        }

        Optional<User> chosen = developers.stream()
                .min(Comparator
                        // Primary sort: fewest open tickets
                        .comparingLong((User dev) ->
                                ticketRepository.countOpenTicketsByAssignee(dev.getId(), CLOSED_STATUSES))
                        // Tie-break: lowest user ID for deterministic results
                        .thenComparingLong(User::getId));

        chosen.ifPresent(dev ->
                log.debug("Auto-assignment: assigned to developer '{}' (id={})", dev.getUsername(), dev.getId()));

        return chosen;
    }
}
