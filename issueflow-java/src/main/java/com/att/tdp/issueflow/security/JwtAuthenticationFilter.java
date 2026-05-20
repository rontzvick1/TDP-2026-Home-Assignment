package com.att.tdp.issueflow.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Intercepts every HTTP request and populates the {@link SecurityContextHolder}
 * when a valid JWT is found in the {@code Authorization} header.
 *
 * <p>Processing flow:</p>
 * <ol>
 *   <li>Extract the token from {@code Authorization: Bearer &lt;token&gt;}.</li>
 *   <li>Delegate signature/expiry validation to {@link JwtTokenProvider}.</li>
 *   <li>If valid, load the {@link UserDetails} and install a
 *       {@link UsernamePasswordAuthenticationToken} in the security context.</li>
 *   <li>Always call {@code filterChain.doFilter()} so the request continues.</li>
 * </ol>
 *
 * <p>If the token is absent, expired, or invalid the security context is left empty
 * and Spring Security's access-control rules will reject the request with 401/403.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider      jwtTokenProvider;
    private final UserDetailsServiceImpl userDetailsService;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest  request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain         filterChain)
            throws ServletException, IOException {

        try {
            String token = extractBearerToken(request);

            if (StringUtils.hasText(token) && jwtTokenProvider.validateToken(token)) {
                String username = jwtTokenProvider.getUsernameFromToken(token);

                // Only set context if not already authenticated (e.g., during test mocking)
                if (SecurityContextHolder.getContext().getAuthentication() == null) {
                    UserDetails userDetails = userDetailsService.loadUserByUsername(username);

                    UsernamePasswordAuthenticationToken authToken =
                            new UsernamePasswordAuthenticationToken(
                                    userDetails,
                                    null,                          // credentials — not needed post-auth
                                    userDetails.getAuthorities()
                            );

                    // Attaches remote address, session id, etc. for auditing
                    authToken.setDetails(
                            new WebAuthenticationDetailsSource().buildDetails(request));

                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            }
        } catch (Exception ex) {
            log.error("Could not set user authentication in security context: {}", ex.getMessage());
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Reads the raw token from {@code Authorization: Bearer <token>}.
     *
     * @return the token string, or {@code null} if the header is absent/malformed
     */
    private String extractBearerToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (StringUtils.hasText(header) && header.startsWith("Bearer ")) {
            return header.substring(7).trim();
        }
        return null;
    }
}
