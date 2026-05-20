package com.att.tdp.issueflow.repository;

import com.att.tdp.issueflow.entity.User;
import com.att.tdp.issueflow.entity.UserRole;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import jakarta.persistence.LockModeType;
import com.att.tdp.issueflow.entity.TicketStatus;

import java.util.List;
import java.util.Optional;

/**
 * Data access layer for {@link User} entities.
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /** Used by login and mention-parsing to look up a user by their unique login name. */
    Optional<User> findByUsername(String username);

    /** Used during user creation to check for duplicate usernames. */
    boolean existsByUsername(String username);

    /** Used during user creation to check for duplicate emails. */
    boolean existsByEmail(String email);

    /** Used by auto-assignment to find all developers in the system. */
    List<User> findAllByRole(UserRole role);

    /** Finds the single least-loaded DEVELOPER directly via DB with a pessimistic write lock */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT u FROM User u
        WHERE u.role = 'DEVELOPER'
        ORDER BY (
            SELECT COUNT(t) FROM Ticket t 
            WHERE t.assignee = u 
              AND t.deletedAt IS NULL 
              AND t.status NOT IN :closedStatuses
        ) ASC, u.id ASC
    """)
    List<User> findLeastLoadedDeveloperWithLock(@Param("closedStatuses") List<TicketStatus> closedStatuses, Pageable pageable);
}
