package com.att.tdp.issueflow.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

/**
 * Response body for {@code POST /auth/login}.
 *
 * <pre>
 * {
 *   "accessToken": "&lt;jwt&gt;",
 *   "tokenType":   "Bearer",
 *   "expiresIn":   3600
 * }
 * </pre>
 */
@Getter
@Builder
public class AuthResponse {

    @JsonProperty("accessToken")
    private final String accessToken;

    /** Always {@code "Bearer"}. */
    @JsonProperty("tokenType")
    private final String tokenType;

    /** Token lifetime in seconds. */
    @JsonProperty("expiresIn")
    private final long expiresIn;
}
