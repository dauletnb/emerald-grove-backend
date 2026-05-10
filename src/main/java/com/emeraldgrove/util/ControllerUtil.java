package com.emeraldgrove.util;

import com.emeraldgrove.entity.User;
import com.emeraldgrove.security.CurrentUserResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Utility class for common controller operations
 */
@Component
@RequiredArgsConstructor
public class ControllerUtil {
    private final CurrentUserResolver currentUserResolver;

    public User getCurrentUser() {
        return currentUserResolver.getCurrentUser();
    }
}