package com.att.tdp.issueflow.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Request body for creating a new comment.
 */
@Getter
@NoArgsConstructor
public class CreateCommentRequest {

    @NotBlank(message = "Content must not be blank")
    private String content;

    private Long authorId; // In a real app this would come from the JWT/SecurityContext, but keeping it explicit for testing/API completeness if needed. Actually, let's keep it in the body per usual REST if not strictly extracted from auth, or we can extract it in the controller.
}
