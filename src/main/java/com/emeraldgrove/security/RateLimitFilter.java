package com.emeraldgrove.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * In-memory fixed-window rate limiting filter.
 * Limits POST /api/articles/sync and GET /api/auth/check per client IP.
 * Returns 429 with JSON body when the limit is exceeded.
 */
public class RateLimitFilter extends OncePerRequestFilter {

    private record WindowEntry(long windowStart, AtomicInteger count) {}

    private final ConcurrentHashMap<String, WindowEntry> windows = new ConcurrentHashMap<>();
    private final int syncLimit;
    private final int authCheckLimit;
    private final long windowMs;

    public RateLimitFilter(int syncLimit, int authCheckLimit, long windowMs) {
        this.syncLimit = syncLimit;
        this.authCheckLimit = authCheckLimit;
        this.windowMs = windowMs;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain chain
    ) throws ServletException, IOException {

        int limit = resolveLimit(request);
        if (limit <= 0) {
            chain.doFilter(request, response);
            return;
        }

        String ip = extractIp(request);
        String key = ip + "|" + request.getMethod() + "|" + request.getRequestURI();
        long now = System.currentTimeMillis();

        WindowEntry entry = windows.compute(key, (k, existing) -> {
            if (existing == null || now - existing.windowStart() >= windowMs) {
                return new WindowEntry(now, new AtomicInteger(0));
            }
            return existing;
        });

        int count = entry.count().incrementAndGet();
        if (count > limit) {
            long retryAfterSec = Math.max(1, (windowMs - (now - entry.windowStart())) / 1000);
            response.setStatus(429);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write(
                    "{\"error\":\"Too many requests\",\"retryAfter\":" + retryAfterSec + "}"
            );
            return;
        }

        chain.doFilter(request, response);
    }

    private int resolveLimit(HttpServletRequest request) {
        String method = request.getMethod();
        String uri = request.getRequestURI();
        if ("POST".equalsIgnoreCase(method) && uri.endsWith("/api/articles/sync")) {
            return syncLimit;
        }
        if ("GET".equalsIgnoreCase(method) && uri.endsWith("/api/auth/check")) {
            return authCheckLimit;
        }
        return 0;
    }

    private String extractIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        String remoteAddr = request.getRemoteAddr();
        return remoteAddr != null ? remoteAddr : "unknown";
    }
}
