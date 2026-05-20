package com.att.tdp.issueflow.controller;

import com.att.tdp.issueflow.dto.request.CreateCommentRequest;
import com.att.tdp.issueflow.dto.request.UpdateCommentRequest;
import com.att.tdp.issueflow.dto.response.CommentResponse;
import com.att.tdp.issueflow.service.CommentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for managing Comments on Tickets.
 *
 * <p>Routes mapped:</p>
 * <ul>
 *   <li>{@code GET /tickets/{ticketId}/comments}</li>
 *   <li>{@code POST /tickets/{ticketId}/comments}</li>
 *   <li>{@code PATCH /comments/{commentId}}</li>
 *   <li>{@code DELETE /comments/{commentId}}</li>
 * </ul>
 */
@RestController
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    // -----------------------------------------------------------------------
    // Ticket-scoped operations
    // -----------------------------------------------------------------------

    @GetMapping("/tickets/{ticketId}/comments")
    public ResponseEntity<List<CommentResponse>> getCommentsByTicket(@PathVariable Long ticketId) {
        return ResponseEntity.ok(commentService.getCommentsByTicket(ticketId));
    }

    @PostMapping("/tickets/{ticketId}/comments")
    public ResponseEntity<CommentResponse> createComment(
            @PathVariable Long ticketId,
            @Valid @RequestBody CreateCommentRequest request) {
        return ResponseEntity.ok(commentService.createComment(ticketId, request));
    }

    // -----------------------------------------------------------------------
    // Global Comment operations
    // -----------------------------------------------------------------------

    @PatchMapping("/comments/{commentId}")
    public ResponseEntity<CommentResponse> updateComment(
            @PathVariable Long commentId,
            @Valid @RequestBody UpdateCommentRequest request,
            Authentication authentication) {
        // authentication.getName() returns the username from the JWT
        return ResponseEntity.ok(commentService.updateComment(commentId, request, authentication.getName()));
    }

    @DeleteMapping("/comments/{commentId}")
    public ResponseEntity<Void> deleteComment(
            @PathVariable Long commentId,
            Authentication authentication) {
        commentService.deleteComment(commentId, authentication.getName());
        return ResponseEntity.ok().build();
    }
}
