package com.att.tdp.issueflow.service;

import com.att.tdp.issueflow.entity.User;
import com.att.tdp.issueflow.entity.UserRole;
import com.att.tdp.issueflow.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link AutoAssignmentService}.
 *
 * <p>AutoAssignmentService now delegates the entire "least loaded" logic to a single
 * repository query ({@code findLeastLoadedDeveloperWithLock}), so the DB query
 * is responsible for ordering by ticket count ASC, then id ASC.
 * These tests stub that query directly and verify the service returns whatever
 * the repository gives it as the top result.</p>
 */
@ExtendWith(MockitoExtension.class)
public class AutoAssignmentServiceTest {

    @Mock
    private UserRepository userRepository;

    // TicketRepository is no longer injected into AutoAssignmentService,
    // so no @Mock is needed here.

    @InjectMocks
    private AutoAssignmentService autoAssignmentService;

    @Test
    void findLeastLoadedDeveloper_NoDevelopers_ReturnsEmpty() {
        // Repository returns empty — no developers exist in the system
        when(userRepository.findLeastLoadedDeveloperWithLock(any(), any()))
                .thenReturn(List.of());

        Optional<User> result = autoAssignmentService.findLeastLoadedDeveloper();

        assertTrue(result.isEmpty(), "Should return empty when no developers exist");
    }

    @Test
    void findLeastLoadedDeveloper_AssignsToDevWithFewestOpenTickets() {
        // The DB query returns developers ordered by ticket count ASC.
        // dev2 (id=2) has the fewest tickets, so the DB returns it first.
        User dev2 = User.builder().id(2L).role(UserRole.DEVELOPER).build();

        when(userRepository.findLeastLoadedDeveloperWithLock(any(), any()))
                .thenReturn(List.of(dev2));

        Optional<User> result = autoAssignmentService.findLeastLoadedDeveloper();

        assertTrue(result.isPresent());
        assertEquals(2L, result.get().getId(), "Should pick dev2 as returned first by the sorted DB query");
    }

    @Test
    void findLeastLoadedDeveloper_TieBreakOnId() {
        // When ticket counts are tied, the DB query orders by id ASC.
        // dev2 has id=5, which is lower than dev1's id=10, so the DB returns dev2 first.
        User dev2 = User.builder().id(5L).role(UserRole.DEVELOPER).build();

        when(userRepository.findLeastLoadedDeveloperWithLock(any(), any()))
                .thenReturn(List.of(dev2));

        Optional<User> result = autoAssignmentService.findLeastLoadedDeveloper();

        assertTrue(result.isPresent());
        assertEquals(5L, result.get().getId(), "Should pick the developer with the lower ID on a tie (enforced by DB ORDER BY id ASC)");
    }
}
