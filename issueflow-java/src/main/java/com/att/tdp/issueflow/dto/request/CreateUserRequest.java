package com.att.tdp.issueflow.dto.request;

import com.att.tdp.issueflow.entity.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Request body for creating a new user.
 */
@Getter
@NoArgsConstructor
public class CreateUserRequest {

    @NotBlank(message = "Username must not be blank")
    @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
    private String username;

    @NotBlank(message = "Email must not be blank")
    @Email(message = "Email must be a valid email address")
    private String email;

    @NotBlank(message = "Full name must not be blank")
    @Size(max = 255, message = "Full name must be at most 255 characters")
    private String fullName;

    @NotNull(message = "Role must not be null")
    private UserRole role;

    @NotBlank(message = "Password must not be blank")
    @Size(min = 6, message = "Password must be at least 6 characters")
    private String password;
}
