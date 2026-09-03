package com.ledger.security.policy;

import java.util.Set;

/**
 * The authenticated caller distilled to what authorization needs: who they are,
 * their granted scopes, and the attribute-based limits carried in their token.
 * maxTxnMinor <= 0 means no per-token amount limit; an empty allowedAccounts
 * means no account restriction.
 */
public record AccessSubject(
        String subject,
        Set<String> scopes,
        long maxTxnMinor,
        Set<String> allowedAccounts) {

    public AccessSubject {
        scopes = Set.copyOf(scopes);
        allowedAccounts = Set.copyOf(allowedAccounts);
    }

    public boolean hasScope(String scope) {
        return scopes.contains(scope);
    }
}
