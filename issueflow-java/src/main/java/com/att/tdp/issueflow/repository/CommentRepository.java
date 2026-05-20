package com.att.tdp.issueflow.repository;

import com.att.tdp.issueflow.entity.Comment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Data access layer for {@link Comment} entities.
 */
@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {

    /** Returns all comments for a given ticket, ordered oldest-first. */
    List<Comment> findByTicketIdOrderByCreatedAtAsc(Long ticketId);

    /** Looks up a specific comment by its own ID and the ticket it belongs to. */
    Optional<Comment> findByIdAndTicketId(Long id, Long ticketId);

    /**
     * Returns paginated comments in which a given user is mentioned.
     * Used by the Mentions API ({@code GET /users/:userId/mentions}).
     */
    @Query("""
            SELECT c FROM Comment c
            JOIN c.mentionedUsers mu
            WHERE mu.id = :userId
            ORDER BY c.createdAt DESC
            """)
    Page<Comment> findByMentionedUserId(@Param("userId") Long userId, Pageable pageable);
}
