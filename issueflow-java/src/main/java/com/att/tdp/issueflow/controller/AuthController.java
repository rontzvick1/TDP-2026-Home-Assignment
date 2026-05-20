package com.att.tdp.issueflow.controller;

import com.att.tdp.issueflow.dto.request.LoginRequest;
import com.att.tdp.issueflow.dto.response.AuthResponse;
import com.att.tdp.issueflow.dto.response.UserResponse;
import com.att.tdp.issueflow.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for authentication operations.
 *
 * <table>
 *   <tr><th>Method</th><th>Endpoint</th><th>Auth</th><th>Description</th></tr>
 *   <tr><td>POST</td><td>/auth/login</td><td>None</td><td>Obtain a JWT</td></tr>
 *   <tr><td>POST</td><td>/auth/logout</td><td>JWT</td><td>Invalidate session (stateless)</td></tr>
 *   <tr><td>GET</td><td>/auth/me</td><td>JWT</td><td>Return current user profile</td></tr>
 * </table>
 */
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    // -----------------------------------------------------------------------
    // POST /auth/login
    // -----------------------------------------------------------------------

    /**
     * Authenticates the user with username + password and returns a signed JWT.
     *
     * <p>This endpoint is open (no JWT required) — it is the only public URL
     * configured in {@code SecurityConfig}.</p>
     *
     * @param request {@code { "username": "jdoe", "password": "secret" }}
     * @return {@code { "accessToken": "...", "tokenType": "Bearer", "expiresIn": 3600 }}
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    // -----------------------------------------------------------------------
    // POST /auth/logout
    // -----------------------------------------------------------------------

    /**
     * Stateless logout — instructs the client to discard the JWT.
     * No server-side state is removed (tokens are validated via signature only).
     *
     * <p>Returns 200 OK with an empty body as per the README specification.</p>
     */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout() {
        // For a stateless JWT system, logout is handled entirely on the client side.
        // Future enhancement: maintain a server-side token blacklist (e.g., Redis set).
        return ResponseEntity.ok().build();
    }

    // -----------------------------------------------------------------------
    // GET /auth/me
    // -----------------------------------------------------------------------

    /**
     * Returns the full profile of the currently authenticated user.
     *
     * <p>The {@link Authentication} argument is automatically populated by
     * Spring Security from the validated JWT in the {@code Authorization} header.</p>
     *
     * @param authentication injected by Spring Security; {@code getName()} returns username
     */
    @GetMapping("/me")
    public ResponseEntity<UserResponse> me(Authentication authentication) {
        return ResponseEntity.ok(authService.getCurrentUser(authentication.getName()));
    }
}
