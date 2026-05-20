package com.att.tdp.issueflow.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Request body for updating a comment.
 */
@Getter
@NoArgsConstructor
public class UpdateCommentRequest {

    @NotBlank(message = "Content must not be blank")
    private String content;
}
