package com.ledger.verify;

import com.ledger.security.policy.AbacDecision;
import com.ledger.security.policy.AbacPolicy;
import com.ledger.security.policy.AccessSubject;
import com.ledger.security.policy.ScopeAuthorities;
import com.ledger.security.ratelimit.TokenBucket;
import java.util.Set;

public final class SecurityVerify {
    private static int passed = 0, failed = 0;

    public static void main(String[] args) {
        scopes();
        abac();
        rateLimit();
        System.out.println();
        System.out.println("SECURITY RESULT: " + passed + " passed, " + failed + " failed");
        if (failed > 0) System.exit(1);
    }

    private static void scopes() {
        var s = ScopeAuthorities.parse("ledger:read ledger:write");
        check("parses space-delimited scopes", s.equals(Set.of("ledger:read", "ledger:write")));
        check("blank scope claim is empty", ScopeAuthorities.parse("  ").isEmpty());
        check("null scope claim is empty", ScopeAuthorities.parse(null).isEmpty());
        check("collapses extra whitespace", ScopeAuthorities.parse("a   b").equals(Set.of("a", "b")));
    }

    private static void abac() {
        var policy = new AbacPolicy();

        var limited = new AccessSubject("u1", Set.of("ledger:write"), 10000, Set.of());
        check("within limit is permitted",
                policy.permitTransaction(limited, Set.of("acc1"), 5000).permitted());
        AbacDecision over = policy.permitTransaction(limited, Set.of("acc1"), 20000);
        check("over limit is denied", !over.permitted());
        check("denial explains why", over.reason().contains("exceeds token limit"));

        var unlimited = new AccessSubject("u2", Set.of("ledger:write"), 0, Set.of());
        check("no limit permits large amount",
                policy.permitTransaction(unlimited, Set.of("acc1"), 999_999_999L).permitted());

        var scoped = new AccessSubject("u3", Set.of("ledger:write"), 0, Set.of("accA", "accB"));
        check("allowed account permitted",
                policy.permitTransaction(scoped, Set.of("accA"), 100).permitted());
        AbacDecision foreign = policy.permitTransaction(scoped, Set.of("accC"), 100);
        check("account outside allowlist denied", !foreign.permitted());
        check("empty allowlist means any account",
                policy.permitTransaction(unlimited, Set.of("anything"), 100).permitted());
    }

    private static void rateLimit() {
        long t0 = 0L;
        var bucket = new TokenBucket(3, 1.0, t0); // capacity 3, refill 1/sec
        check("first consume ok", bucket.tryConsume(t0));
        check("second consume ok", bucket.tryConsume(t0));
        check("third consume ok", bucket.tryConsume(t0));
        check("fourth consume denied (bucket empty)", !bucket.tryConsume(t0));

        long oneSecLater = t0 + 1_000_000_000L;
        check("refills one token after one second", bucket.tryConsume(oneSecLater));
        check("but not two", !bucket.tryConsume(oneSecLater));

        long tenSecLater = t0 + 10_000_000_000L;
        check("refill caps at capacity", bucket.tryConsume(tenSecLater)
                && bucket.tryConsume(tenSecLater) && bucket.tryConsume(tenSecLater));
        check("capacity not exceeded", !bucket.tryConsume(tenSecLater));
    }

    private static void check(String name, boolean ok) {
        if (ok) { passed++; System.out.println("  PASS  " + name); }
        else { failed++; System.out.println("  FAIL  " + name); }
    }
}
