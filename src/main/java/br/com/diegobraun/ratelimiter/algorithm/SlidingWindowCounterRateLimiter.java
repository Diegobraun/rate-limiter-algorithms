package br.com.diegobraun.ratelimiter.algorithm;

import br.com.diegobraun.ratelimiter.core.Algorithm;
import br.com.diegobraun.ratelimiter.core.KeyedStateRateLimiter;
import br.com.diegobraun.ratelimiter.core.RateLimitConfig;
import br.com.diegobraun.ratelimiter.core.RateLimitDecision;
import br.com.diegobraun.ratelimiter.core.Ticker;

public final class SlidingWindowCounterRateLimiter extends KeyedStateRateLimiter<SlidingWindowCounterRateLimiter.Counters> {

    public SlidingWindowCounterRateLimiter(RateLimitConfig config, Ticker ticker) {
        super(config, ticker);
    }

    public SlidingWindowCounterRateLimiter(RateLimitConfig config) {
        this(config, Ticker.system());
    }

    @Override
    public Algorithm algorithm() {
        return Algorithm.SLIDING_WINDOW_COUNTER;
    }

    @Override
    protected Counters newState(long now) {
        return new Counters(windowStart(now));
    }

    @Override
    protected RateLimitDecision acquire(Counters counters, long now) {
        long period = config.periodNanos();
        long limit = config.limit();
        long currentStart = windowStart(now);

        if (currentStart == counters.windowStart + period) {
            counters.previous = counters.current;
            counters.current = 0;
        } else if (currentStart != counters.windowStart) {
            counters.previous = 0;
            counters.current = 0;
        }
        counters.windowStart = currentStart;

        double elapsedFraction = (double) (now - currentStart) / period;
        double estimated = counters.previous * (1.0 - elapsedFraction) + counters.current;

        if (estimated + 1 <= limit) {
            counters.current++;
            return RateLimitDecision.allow(limit, (long) Math.floor(limit - estimated - 1));
        }
        return RateLimitDecision.reject(limit, nanosUntilAllowed(counters, now));
    }

    private long nanosUntilAllowed(Counters counters, long now) {
        long period = config.periodNanos();
        long limit = config.limit();
        if (counters.current + 1 <= limit && counters.previous > 0) {
            double requiredFraction = 1.0 - (double) (limit - counters.current - 1) / counters.previous;
            return (long) Math.ceil(counters.windowStart + requiredFraction * period - now);
        }
        double requiredFraction = Math.max(0.0, 1.0 - (double) (limit - 1) / counters.current);
        return (long) Math.ceil(counters.windowStart + period + requiredFraction * period - now);
    }

    private long windowStart(long now) {
        return now - Math.floorMod(now, config.periodNanos());
    }

    static final class Counters {
        private long windowStart;
        private long previous;
        private long current;

        private Counters(long windowStart) {
            this.windowStart = windowStart;
        }
    }
}
