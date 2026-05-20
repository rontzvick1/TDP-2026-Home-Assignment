package com.att.tdp.issueflow.repository;

import com.att.tdp.issueflow.entity.Attachment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Data access layer for {@link Attachment} entities.
 */
@Repository
public interface AttachmentRepository extends JpaRepository<Attachment, Long> {

    /**
     * Finds an attachment by its own ID and the ticket it belongs to.
     * Used to validate ownership before deletion.
     */
    Optional<Attachment> findByIdAndTicketId(Long id, Long ticketId);
}
