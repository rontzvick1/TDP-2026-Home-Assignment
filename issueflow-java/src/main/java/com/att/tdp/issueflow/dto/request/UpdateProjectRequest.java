package com.att.tdp.issueflow.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Request body for updating an existing project.
 */
@Getter
@NoArgsConstructor
public class UpdateProjectRequest {

    @Size(max = 255, message = "Name must be at most 255 characters")
    private String name;

    private String description;
}
