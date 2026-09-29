package br.com.diegobraun.ratelimiter.core;

import java.time.Duration;

public record RateLimitDecision(boolean allowed, long limit, long remaining, Duration retryAfter, Duration delay) {

    public static RateLimitDecision allow(long limit, long remaining) {
        return new RateLimitDecision(true, limit, Math.max(0, remaining), Duration.ZERO, Duration.ZERO);
    }

    public static RateLimitDecision allowAfter(long limit, long remaining, long delayNanos) {
        return new RateLimitDecision(true, limit, Math.max(0, remaining), Duration.ZERO, Duration.ofNanos(Math.max(0, delayNanos)));
    }

    public static RateLimitDecision reject(long limit, long retryAfterNanos) {
        return new RateLimitDecision(false, limit, 0, Duration.ofNanos(Math.max(1, retryAfterNanos)), Duration.ZERO);
    }
}
