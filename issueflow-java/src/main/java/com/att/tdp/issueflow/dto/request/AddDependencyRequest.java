package com.att.tdp.issueflow.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Request body for adding a ticket dependency.
 *
 * <p>Semantics: "ticketId is blocked by blockedByTicketId".</p>
 */
@Getter
@Setter
@NoArgsConstructor
public class AddDependencyRequest {

    @NotNull(message = "blockedByTicketId must not be null")
    private Long blockedByTicketId;
}
