package com.emeraldgrove.security;

import com.emeraldgrove.entity.User;
import com.emeraldgrove.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Helper that reads the currently authenticated user from Spring Security context.
 *
 * After a JWT is validated in the filter, Spring Security stores the user's email
 * as the "principal" (the identity). This class looks up the full User record by that email.
 */
@Component
@RequiredArgsConstructor
public class CurrentUserResolver {
    private final UserRepository userRepository;

    public User getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
            .orElseThrow(() -> new IllegalStateException("Authenticated user not found in database: " + email));
    }
}
