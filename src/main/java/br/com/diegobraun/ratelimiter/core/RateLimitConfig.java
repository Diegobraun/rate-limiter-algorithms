package br.com.diegobraun.ratelimiter.core;

import java.time.Duration;
import java.util.Objects;

public record RateLimitConfig(long limit, Duration period) {

    public RateLimitConfig {
        Objects.requireNonNull(period, "period");
        if (limit <= 0) {
            throw new IllegalArgumentException("limit must be positive");
        }
        if (period.isNegative() || period.isZero()) {
            throw new IllegalArgumentException("period must be positive");
        }
    }

    public static RateLimitConfig of(long limit, Duration period) {
        return new RateLimitConfig(limit, period);
    }

    public long periodNanos() {
        return period.toNanos();
    }

    public long emissionIntervalNanos() {
        return Math.max(1, Math.round((double) periodNanos() / limit));
    }

    public double ratePerNano() {
        return (double) limit / periodNanos();
    }
}
