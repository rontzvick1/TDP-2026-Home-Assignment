package com.att.tdp.issueflow.dto.response;

import com.att.tdp.issueflow.entity.Dependency;
import lombok.Builder;
import lombok.Getter;

/**
 * Public view of a {@link Dependency} edge.
 *
 * <p>Each entry represents: "ticketId is blocked by blockedByTicketId".</p>
 */
@Getter
@Builder
public class DependencyResponse {

    private final Long id;
    private final Long ticketId;
    private final Long blockedByTicketId;

    public static DependencyResponse fromEntity(Dependency dependency) {
        return DependencyResponse.builder()
                .id(dependency.getId())
                .ticketId(dependency.getTicket().getId())
                .blockedByTicketId(dependency.getBlocker().getId())
                .build();
    }
}
