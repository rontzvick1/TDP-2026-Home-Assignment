package com.att.tdp.issueflow.service;

import com.att.tdp.issueflow.dto.request.LoginRequest;
import com.att.tdp.issueflow.dto.response.AuthResponse;
import com.att.tdp.issueflow.dto.response.UserResponse;
import com.att.tdp.issueflow.entity.User;
import com.att.tdp.issueflow.exception.NotFoundException;
import com.att.tdp.issueflow.exception.ValidationException;
import com.att.tdp.issueflow.repository.UserRepository;
import com.att.tdp.issueflow.security.JwtTokenProvider;
import com.att.tdp.issueflow.security.UserDetailsServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Business logic for authentication operations.
 *
 * <h3>Login flow</h3>
 * <ol>
 *   <li>Load the {@link User} by username (returns generic error if not found —
 *       no username-enumeration leakage).</li>
 *   <li>Verify the plain-text password against the stored BCrypt hash.</li>
 *   <li>Generate a signed JWT and return it with metadata.</li>
 * </ol>
 *
 * <h3>Logout</h3>
 * Stateless: the controller returns 200 OK; the client is responsible for
 * discarding the token. An optional in-memory token blacklist may be added later.
 *
 * <h3>Current user</h3>
 * Resolves the principal name from the {@code SecurityContext} (set by
 * {@code JwtAuthenticationFilter}) and returns the full user profile.
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository         userRepository;
    private final PasswordEncoder        passwordEncoder;
    private final JwtTokenProvider       jwtTokenProvider;
    private final UserDetailsServiceImpl userDetailsService;

    @Value("${app.jwt.expiration-ms}")
    private long jwtExpirationMs;

    // -----------------------------------------------------------------------
    // Login
    // -----------------------------------------------------------------------

    /**
     * Authenticates the user and returns a JWT.
     *
     * @throws ValidationException with a deliberately vague message to prevent
     *                             username enumeration attacks
     */
    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        // Use the same error message whether the user doesn't exist OR the password is wrong
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new ValidationException("Invalid username or password"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new ValidationException("Invalid username or password");
        }

        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getUsername());
        String token = jwtTokenProvider.generateToken(userDetails);

        return AuthResponse.builder()
                .accessToken(token)
                .tokenType("Bearer")
                .expiresIn(jwtExpirationMs / 1000)   // convert ms → seconds
                .build();
    }

    // -----------------------------------------------------------------------
    // Current user
    // -----------------------------------------------------------------------

    /**
     * Returns the profile of the currently authenticated user.
     *
     * @param username extracted from the validated JWT by {@code JwtAuthenticationFilter}
     */
    @Transactional(readOnly = true)
    public UserResponse getCurrentUser(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new NotFoundException("User not found: " + username));
        return UserResponse.fromEntity(user);
    }
}
