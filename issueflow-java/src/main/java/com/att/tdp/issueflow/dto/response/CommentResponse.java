package com.att.tdp.issueflow.dto.response;

import com.att.tdp.issueflow.entity.Comment;
import com.att.tdp.issueflow.entity.User;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Public view of a {@link Comment}.
 */
@Getter
@Builder
public class CommentResponse {

    private final Long id;
    private final Long ticketId;
    private final Long authorId;
    private final String authorUsername;
    private final String content;
    private final List<String> mentionedUsernames;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'", timezone = "UTC")
    private final Instant createdAt;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'", timezone = "UTC")
    private final Instant updatedAt;

    public static CommentResponse fromEntity(Comment comment) {
        return CommentResponse.builder()
                .id(comment.getId())
                .ticketId(comment.getTicket().getId())
                .authorId(comment.getAuthor().getId())
                .authorUsername(comment.getAuthor().getUsername())
                .content(comment.getContent())
                .mentionedUsernames(comment.getMentionedUsers().stream()
                        .map(User::getUsername)
                        .collect(Collectors.toList()))
                .createdAt(comment.getCreatedAt())
                .updatedAt(comment.getUpdatedAt())
                .build();
    }
}
