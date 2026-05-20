package com.att.tdp.issueflow.service;

import com.att.tdp.issueflow.entity.User;
import com.att.tdp.issueflow.entity.UserRole;
import com.att.tdp.issueflow.repository.TicketRepository;
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
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class AutoAssignmentServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private TicketRepository ticketRepository;

    @InjectMocks
    private AutoAssignmentService autoAssignmentService;

    @Test
    void findLeastLoadedDeveloper_NoDevelopers_ReturnsEmpty() {
        when(userRepository.findAllByRole(UserRole.DEVELOPER)).thenReturn(List.of());
        
        Optional<User> result = autoAssignmentService.findLeastLoadedDeveloper();
        assertTrue(result.isEmpty());
    }

    @Test
    void findLeastLoadedDeveloper_AssignsToDevWithFewestOpenTickets() {
        User dev1 = User.builder().id(1L).build();
        User dev2 = User.builder().id(2L).build();
        User dev3 = User.builder().id(3L).build();

        when(userRepository.findAllByRole(UserRole.DEVELOPER)).thenReturn(List.of(dev1, dev2, dev3));

        // dev1 has 5 tickets
        when(ticketRepository.countOpenTicketsByAssignee(eq(1L), anyList())).thenReturn(5L);
        // dev2 has 2 tickets
        when(ticketRepository.countOpenTicketsByAssignee(eq(2L), anyList())).thenReturn(2L);
        // dev3 has 10 tickets
        when(ticketRepository.countOpenTicketsByAssignee(eq(3L), anyList())).thenReturn(10L);

        Optional<User> result = autoAssignmentService.findLeastLoadedDeveloper();
        assertTrue(result.isPresent());
        assertEquals(2L, result.get().getId(), "Should pick dev2 as they have the fewest tickets");
    }

    @Test
    void findLeastLoadedDeveloper_TieBreakOnId() {
        User dev1 = User.builder().id(10L).build();
        User dev2 = User.builder().id(5L).build(); // lower ID

        when(userRepository.findAllByRole(UserRole.DEVELOPER)).thenReturn(List.of(dev1, dev2));

        // Both have 3 tickets
        when(ticketRepository.countOpenTicketsByAssignee(eq(10L), anyList())).thenReturn(3L);
        when(ticketRepository.countOpenTicketsByAssignee(eq(5L), anyList())).thenReturn(3L);

        Optional<User> result = autoAssignmentService.findLeastLoadedDeveloper();
        assertTrue(result.isPresent());
        assertEquals(5L, result.get().getId(), "Should pick dev2 due to tie-break on lower ID");
    }
}
