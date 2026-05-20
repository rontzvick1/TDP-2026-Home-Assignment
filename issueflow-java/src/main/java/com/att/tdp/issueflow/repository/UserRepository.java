package com.att.tdp.issueflow.repository;

import com.att.tdp.issueflow.entity.User;
import com.att.tdp.issueflow.entity.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

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
}
