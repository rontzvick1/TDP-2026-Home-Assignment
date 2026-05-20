package com.att.tdp.issueflow.service;

import com.att.tdp.issueflow.dto.request.CreateCommentRequest;
import com.att.tdp.issueflow.dto.request.UpdateCommentRequest;
import com.att.tdp.issueflow.dto.response.CommentResponse;
import com.att.tdp.issueflow.entity.Comment;
import com.att.tdp.issueflow.entity.Ticket;
import com.att.tdp.issueflow.entity.User;
import com.att.tdp.issueflow.annotation.Auditable;
import com.att.tdp.issueflow.entity.AuditAction;
import com.att.tdp.issueflow.entity.EntityType;
import com.att.tdp.issueflow.exception.ForbiddenException;
import com.att.tdp.issueflow.exception.NotFoundException;
import com.att.tdp.issueflow.repository.CommentRepository;
import com.att.tdp.issueflow.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Business logic for Comment management.
 */
@Service
@RequiredArgsConstructor
public class CommentService {

    private final CommentRepository commentRepository;
    private final TicketService ticketService;
    private final UserRepository userRepository;
    private final MentionService mentionService;

    @Transactional(readOnly = true)
    public List<CommentResponse> getCommentsByTicket(Long ticketId) {
        // Validate ticket exists and is active
        ticketService.getActiveTicketEntity(ticketId);

        return commentRepository.findByTicketIdOrderByCreatedAtAsc(ticketId).stream()
                .map(CommentResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional
    @Auditable(action = AuditAction.CREATE, entityType = EntityType.COMMENT)
    public CommentResponse createComment(Long ticketId, CreateCommentRequest request) {
        Ticket ticket = ticketService.getActiveTicketEntity(ticketId);
        User author = userRepository.findById(request.getAuthorId())
                .orElseThrow(() -> new NotFoundException("Author User", request.getAuthorId()));

        Set<User> mentionedUsers = mentionService.extractMentionedUsers(request.getContent());

        Comment comment = Comment.builder()
                .ticket(ticket)
                .author(author)
                .content(request.getContent())
                .mentionedUsers(mentionedUsers)
                .build();

        comment = commentRepository.save(comment);
        return CommentResponse.fromEntity(comment);
    }

    @Transactional
    @Auditable(action = AuditAction.UPDATE, entityType = EntityType.COMMENT)
    public CommentResponse updateComment(Long commentId, UpdateCommentRequest request, String currentUsername) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new NotFoundException("Comment", commentId));

        // Enforce ownership: only the author can edit their comment
        if (!comment.getAuthor().getUsername().equals(currentUsername)) {
            throw new ForbiddenException("You can only edit your own comments");
        }

        comment.setContent(request.getContent());
        comment.setMentionedUsers(mentionService.extractMentionedUsers(request.getContent()));

        comment = commentRepository.save(comment);
        return CommentResponse.fromEntity(comment);
    }

    @Transactional
    @Auditable(action = AuditAction.DELETE, entityType = EntityType.COMMENT)
    public void deleteComment(Long commentId, String currentUsername) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new NotFoundException("Comment", commentId));

        // Enforce ownership: only the author can delete their comment
        // Admin could also be allowed, but per standard rules, usually author only or admin. 
        // We'll check author here. If ADMIN override is needed, we could inject role check.
        if (!comment.getAuthor().getUsername().equals(currentUsername)) {
            User currentUser = userRepository.findByUsername(currentUsername).orElseThrow();
            if (currentUser.getRole() != com.att.tdp.issueflow.entity.UserRole.ADMIN) {
                throw new ForbiddenException("You can only delete your own comments");
            }
        }

        commentRepository.delete(comment);
    }

    // -----------------------------------------------------------------------
    // Mentions logic
    // -----------------------------------------------------------------------

    @Transactional(readOnly = true)
    public Page<CommentResponse> getMentionsForUser(Long userId, Pageable pageable) {
        // Validate user exists
        if (!userRepository.existsById(userId)) {
            throw new NotFoundException("User", userId);
        }

        return commentRepository.findByMentionedUserId(userId, pageable)
                .map(CommentResponse::fromEntity);
    }
}
