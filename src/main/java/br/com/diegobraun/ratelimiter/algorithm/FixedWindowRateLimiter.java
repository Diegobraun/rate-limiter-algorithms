package br.com.diegobraun.ratelimiter.algorithm;

import br.com.diegobraun.ratelimiter.core.Algorithm;
import br.com.diegobraun.ratelimiter.core.KeyedStateRateLimiter;
import br.com.diegobraun.ratelimiter.core.RateLimitConfig;
import br.com.diegobraun.ratelimiter.core.RateLimitDecision;
import br.com.diegobraun.ratelimiter.core.Ticker;

public final class FixedWindowRateLimiter extends KeyedStateRateLimiter<FixedWindowRateLimiter.Window> {

    public FixedWindowRateLimiter(RateLimitConfig config, Ticker ticker) {
        super(config, ticker);
    }

    public FixedWindowRateLimiter(RateLimitConfig config) {
        this(config, Ticker.system());
    }

    @Override
    public Algorithm algorithm() {
        return Algorithm.FIXED_WINDOW;
    }

    @Override
    protected Window newState(long now) {
        return new Window(windowStart(now));
    }

    @Override
    protected RateLimitDecision acquire(Window window, long now) {
        long currentStart = windowStart(now);
        if (currentStart != window.start) {
            window.start = currentStart;
            window.count = 0;
        }
        if (window.count < config.limit()) {
            window.count++;
            return RateLimitDecision.allow(config.limit(), config.limit() - window.count);
        }
        return RateLimitDecision.reject(config.limit(), window.start + config.periodNanos() - now);
    }

    private long windowStart(long now) {
        return now - Math.floorMod(now, config.periodNanos());
    }

    static final class Window {
        private long start;
        private long count;

        private Window(long start) {
            this.start = start;
        }
    }
}
