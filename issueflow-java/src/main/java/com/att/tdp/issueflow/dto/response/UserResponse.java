package com.att.tdp.issueflow.dto.response;

import com.att.tdp.issueflow.entity.User;
import com.att.tdp.issueflow.entity.UserRole;
import lombok.Builder;
import lombok.Getter;

/**
 * Public view of a {@link User} — never exposes {@code password}.
 * Used by {@code GET /auth/me} (Phase 3) and the full Users API (Phase 4).
 *
 * <pre>
 * {
 *   "id":       1,
 *   "username": "jdoe",
 *   "email":    "jdoe@example.com",
 *   "fullName": "John Doe",
 *   "role":     "DEVELOPER"
 * }
 * </pre>
 */
@Getter
@Builder
public class UserResponse {

    private final Long     id;
    private final String   username;
    private final String   email;
    private final String   fullName;
    private final UserRole role;

    /** Convenience factory that maps a {@link User} entity to this DTO. */
    public static UserResponse fromEntity(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(user.getRole())
                .build();
    }
}
