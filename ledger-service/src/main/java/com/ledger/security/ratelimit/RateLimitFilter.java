package com.ledger.security.ratelimit;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Rate-limits write requests, keyed by the authenticated subject. Installed after
 * authentication in the security chain so the subject is available. Returns 429
 * when a caller exhausts their bucket.
 */
public class RateLimitFilter extends OncePerRequestFilter {

    private final RateLimiter rateLimiter;

    public RateLimitFilter(RateLimiter rateLimiter) {
        this.rateLimiter = rateLimiter;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // Only limit money-movement and account-creation writes.
        return !("POST".equalsIgnoreCase(request.getMethod())
                && request.getRequestURI().startsWith("/v1/"));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String key = keyFor();
        if (!rateLimiter.allow(key)) {
            response.setStatus(429);
            response.setContentType("application/json");
            response.getWriter().write(
                    "{\"code\":\"RATE_LIMITED\",\"message\":\"rate limit exceeded\"}");
            return;
        }
        chain.doFilter(request, response);
    }

    private String keyFor() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && auth.getName() != null) {
            return auth.getName();
        }
        return "anonymous";
    }
}
