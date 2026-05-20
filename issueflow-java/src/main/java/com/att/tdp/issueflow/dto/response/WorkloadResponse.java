package com.att.tdp.issueflow.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/**
 * Response body for the workload endpoint.
 */
@Getter
@Builder
@AllArgsConstructor
public class WorkloadResponse {

    private final Long userId;
    private final String username;
    private final long openTicketCount;
}
