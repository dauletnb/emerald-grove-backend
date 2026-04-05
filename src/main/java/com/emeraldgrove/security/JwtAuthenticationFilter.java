package com.emeraldgrove.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Bearer token filter for extension requests.
 * Uses one configured shared token for the diploma version.
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final String configuredExtensionToken;

    public JwtAuthenticationFilter(String configuredExtensionToken) {
        this.configuredExtensionToken = configuredExtensionToken;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        final String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        final String token = authHeader.substring(7).trim();

        try {
            if (isValidExtensionToken(token)) {
                UsernamePasswordAuthenticationToken authToken =
                    new UsernamePasswordAuthenticationToken(
                        "extension-user",
                        null,
                        Collections.emptyList()
                    );

                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        } catch (Exception e) {
            logger.warn("Bearer token validation failed: " + e.getMessage());
        }

        filterChain.doFilter(request, response);
    }

    private boolean isValidExtensionToken(String token) {
        return configuredExtensionToken != null
            && !configuredExtensionToken.isBlank()
            && configuredExtensionToken.equals(token);
    }
}
