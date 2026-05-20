package com.att.tdp.issueflow.dto.request;

import com.att.tdp.issueflow.entity.UserRole;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Request body for updating an existing user.
 * Fields are optional; only provided fields will be updated.
 */
@Getter
@NoArgsConstructor
public class UpdateUserRequest {

    @Size(max = 255, message = "Full name must be at most 255 characters")
    private String fullName;

    private UserRole role;
}
