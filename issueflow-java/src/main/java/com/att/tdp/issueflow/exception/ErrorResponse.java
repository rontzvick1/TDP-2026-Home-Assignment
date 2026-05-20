package com.att.tdp.issueflow.exception;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

/**
 * Standardised JSON error body returned by {@link GlobalExceptionHandler} for every error response.
 *
 * <pre>
 * {
 *   "error":     "NOT_FOUND",
 *   "message":   "Ticket with id 42 not found",
 *   "timestamp": "2026-05-20T10:00:00Z"
 * }
 * </pre>
 */
@Getter
@Builder
public class ErrorResponse {

    /** Short uppercase error code matching the HTTP status reason (e.g., NOT_FOUND, CONFLICT). */
    private final String error;

    /** Human-readable description of what went wrong. */
    private final String message;

    /** UTC timestamp of when the error occurred. */
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'", timezone = "UTC")
    private final Instant timestamp;
}
