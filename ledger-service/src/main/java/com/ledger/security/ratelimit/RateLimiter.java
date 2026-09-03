package com.ledger.security.ratelimit;

import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Per-caller token-bucket rate limiter. One bucket per key (the authenticated
 * subject), created on first use. The bucket algorithm is the pure, verified
 * TokenBucket; this holds the buckets and the system clock.
 */
@Component
public class RateLimiter {

    private final ConcurrentHashMap<String, TokenBucket> buckets = new ConcurrentHashMap<>();
    private final long capacity;
    private final double refillPerSecond;

    public RateLimiter(@Value("${security.rate-limit.capacity:20}") long capacity,
                       @Value("${security.rate-limit.refill-per-second:10}") double refillPerSecond) {
        this.capacity = capacity;
        this.refillPerSecond = refillPerSecond;
    }

    public boolean allow(String key) {
        long now = System.nanoTime();
        return buckets.computeIfAbsent(key, k -> new TokenBucket(capacity, refillPerSecond, now))
                .tryConsume(now);
    }
}
