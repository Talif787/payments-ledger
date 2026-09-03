package com.ledger.security.policy;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Parses an OAuth2 scope claim into a normalized set of scopes. Mirrors the
 * conventional space-delimited scope encoding so authorization is testable
 * independently of the framework's converter.
 */
public final class ScopeAuthorities {

    private ScopeAuthorities() {}

    public static Set<String> parse(String scopeClaim) {
        Set<String> scopes = new LinkedHashSet<>();
        if (scopeClaim == null || scopeClaim.isBlank()) {
            return scopes;
        }
        for (String part : scopeClaim.trim().split("\\s+")) {
            if (!part.isBlank()) {
                scopes.add(part);
            }
        }
        return scopes;
    }
}
