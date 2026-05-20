package com.att.tdp.issueflow.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;

/**
 * Handles all JWT operations: token generation, validation, and claim extraction.
 *
 * <p>Uses the {@code jjwt 0.12.x} API with HS256 (HMAC-SHA-256).
 * The signing key is read from {@code app.jwt.secret} in {@code application.yaml}
 * and must be at least 256 bits (32 bytes) after Base64 decoding.</p>
 *
 * <p>Token payload claims:</p>
 * <ul>
 *   <li>{@code sub}  – username</li>
 *   <li>{@code role} – e.g. {@code "ROLE_ADMIN"} or {@code "ROLE_DEVELOPER"}</li>
 *   <li>{@code iat}  – issued-at timestamp</li>
 *   <li>{@code exp}  – expiry timestamp</li>
 * </ul>
 */
@Slf4j
@Component
public class JwtTokenProvider {

    @Value("${app.jwt.secret}")
    private String jwtSecret;

    @Value("${app.jwt.expiration-ms}")
    private long jwtExpirationMs;

    // -----------------------------------------------------------------------
    // Token generation
    // -----------------------------------------------------------------------

    /**
     * Generates a signed JWT for the given user.
     *
     * @param userDetails the authenticated user's Spring Security principal
     * @return compact, URL-safe JWT string
     */
    public String generateToken(UserDetails userDetails) {
        String role = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .findFirst()
                .orElse("");

        long now = System.currentTimeMillis();
        return Jwts.builder()
                .subject(userDetails.getUsername())
                .claim("role", role)
                .issuedAt(new Date(now))
                .expiration(new Date(now + jwtExpirationMs))
                .signWith(getSigningKey())
                .compact();
    }

    // -----------------------------------------------------------------------
    // Token validation & claim extraction
    // -----------------------------------------------------------------------

    /**
     * Returns {@code true} if the token has a valid signature and has not expired.
     */
    public boolean validateToken(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (ExpiredJwtException e) {
            log.warn("JWT token is expired: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            log.warn("JWT token is unsupported: {}", e.getMessage());
        } catch (MalformedJwtException e) {
            log.warn("JWT token is malformed: {}", e.getMessage());
        } catch (SecurityException e) {
            log.warn("JWT signature validation failed: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            log.warn("JWT token claims string is empty: {}", e.getMessage());
        }
        return false;
    }

    /**
     * Extracts the {@code sub} (username) claim from a validated token.
     */
    public String getUsernameFromToken(String token) {
        return parseClaims(token).getSubject();
    }

    // -----------------------------------------------------------------------
    // Internal helpers
    // -----------------------------------------------------------------------

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * Decodes the Base64-encoded secret from config and wraps it in a
     * {@link SecretKey} suitable for HS256 signing/verification.
     */
    private SecretKey getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(jwtSecret);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
