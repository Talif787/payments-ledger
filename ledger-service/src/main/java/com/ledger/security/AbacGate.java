package com.ledger.security;

import com.ledger.domain.money.Currency;
import com.ledger.domain.money.Money;
import com.ledger.presentation.web.dto.PostTransactionRequest;
import com.ledger.presentation.web.dto.PostingDto;
import com.ledger.security.policy.AbacDecision;
import com.ledger.security.policy.AbacPolicy;
import com.ledger.security.policy.AccessSubject;
import com.ledger.security.policy.ScopeAuthorities;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

/**
 * Attribute-based check at the API boundary: enforces the amount ceiling and
 * account allowlist carried in the caller's own token. When there is no JWT (for
 * example security disabled), it is a no-op, so RBAC alone governs.
 */
@Component
public class AbacGate {

    private final AbacPolicy policy = new AbacPolicy();

    public void check(PostTransactionRequest request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (!(auth instanceof JwtAuthenticationToken jwtAuth)) {
            return;
        }
        Jwt jwt = jwtAuth.getToken();
        AccessSubject subject = new AccessSubject(
                jwt.getSubject(),
                ScopeAuthorities.parse(jwt.getClaimAsString("scope")),
                claimLong(jwt, "max_txn_minor"),
                claimSet(jwt, "accounts"));

        Set<String> debitAccounts = new LinkedHashSet<>();
        long totalDebitMinor = 0L;
        for (PostingDto p : request.postings()) {
            if ("DEBIT".equalsIgnoreCase(p.direction())) {
                debitAccounts.add(p.accountId());
                totalDebitMinor += Money.of(p.amount(), Currency.of(p.currency())).minorUnits();
            }
        }

        AbacDecision decision = policy.permitTransaction(subject, debitAccounts, totalDebitMinor);
        if (!decision.permitted()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "ABAC_DENIED: " + decision.reason());
        }
    }

    private long claimLong(Jwt jwt, String name) {
        Object v = jwt.getClaim(name);
        if (v instanceof Number n) return n.longValue();
        if (v instanceof String s && !s.isBlank()) {
            try { return Long.parseLong(s.trim()); } catch (NumberFormatException ignored) { }
        }
        return 0L;
    }

    private Set<String> claimSet(Jwt jwt, String name) {
        Object v = jwt.getClaim(name);
        Set<String> out = new LinkedHashSet<>();
        if (v instanceof Collection<?> c) {
            for (Object o : c) if (o != null) out.add(o.toString());
        } else if (v instanceof String s && !s.isBlank()) {
            for (String part : s.trim().split("\\s+")) out.add(part);
        }
        return out;
    }
}
