package com.ledger.security.ratelimit;

/**
 * A classic token bucket. Tokens refill continuously at a fixed rate up to a
 * capacity; each request consumes one. Time is passed in explicitly (nanoseconds)
 * so the algorithm is pure and testable with a controllable clock.
 */
public final class TokenBucket {

    private final double capacity;
    private final double refillPerSecond;
    private double tokens;
    private long lastRefillNanos;

    public TokenBucket(long capacity, double refillPerSecond, long nowNanos) {
        this.capacity = capacity;
        this.refillPerSecond = refillPerSecond;
        this.tokens = capacity;
        this.lastRefillNanos = nowNanos;
    }

    public synchronized boolean tryConsume(long nowNanos) {
        refill(nowNanos);
        if (tokens >= 1.0) {
            tokens -= 1.0;
            return true;
        }
        return false;
    }

    private void refill(long nowNanos) {
        if (nowNanos <= lastRefillNanos) {
            return;
        }
        double elapsedSeconds = (nowNanos - lastRefillNanos) / 1_000_000_000.0;
        tokens = Math.min(capacity, tokens + elapsedSeconds * refillPerSecond);
        lastRefillNanos = nowNanos;
    }
}
