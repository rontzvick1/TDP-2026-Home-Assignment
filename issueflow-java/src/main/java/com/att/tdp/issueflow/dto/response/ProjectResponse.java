package com.att.tdp.issueflow.dto.response;

import com.att.tdp.issueflow.entity.Project;
import lombok.Builder;
import lombok.Getter;

/**
 * Public view of a {@link Project}.
 */
@Getter
@Builder
public class ProjectResponse {

    private final Long id;
    private final String name;
    private final String description;
    private final Long ownerId;

    public static ProjectResponse fromEntity(Project project) {
        return ProjectResponse.builder()
                .id(project.getId())
                .name(project.getName())
                .description(project.getDescription())
                .ownerId(project.getOwner().getId())
                .build();
    }
}
