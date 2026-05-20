package com.att.tdp.issueflow.security;

import com.att.tdp.issueflow.repository.UserRepository;
import com.att.tdp.issueflow.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Bridges the application's {@link User} entity with Spring Security's
 * {@link UserDetailsService} contract.
 *
 * <p>Called by both the {@link JwtAuthenticationFilter} (on every authenticated request)
 * and the login flow in {@code AuthService}.</p>
 *
 * <p>Role mapping: {@code UserRole.ADMIN} → authority {@code "ROLE_ADMIN"},
 * {@code UserRole.DEVELOPER} → authority {@code "ROLE_DEVELOPER"}.</p>
 */
@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;

    /**
     * Loads a Spring Security {@link UserDetails} by username.
     *
     * @throws UsernameNotFoundException if no active user exists with this username
     */
    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "No user found with username: " + username));

        // "ROLE_" prefix is the Spring Security convention for role-based authorities.
        String authority = "ROLE_" + user.getRole().name();   // e.g. ROLE_ADMIN

        return new org.springframework.security.core.userdetails.User(
                user.getUsername(),
                user.getPassword(),
                List.of(new SimpleGrantedAuthority(authority))
        );
    }
}
