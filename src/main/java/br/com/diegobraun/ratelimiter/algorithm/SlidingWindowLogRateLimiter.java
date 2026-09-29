package br.com.diegobraun.ratelimiter.algorithm;

import br.com.diegobraun.ratelimiter.core.Algorithm;
import br.com.diegobraun.ratelimiter.core.KeyedStateRateLimiter;
import br.com.diegobraun.ratelimiter.core.RateLimitConfig;
import br.com.diegobraun.ratelimiter.core.RateLimitDecision;
import br.com.diegobraun.ratelimiter.core.Ticker;

import java.util.ArrayDeque;

public final class SlidingWindowLogRateLimiter extends KeyedStateRateLimiter<ArrayDeque<Long>> {

    public SlidingWindowLogRateLimiter(RateLimitConfig config, Ticker ticker) {
        super(config, ticker);
    }

    public SlidingWindowLogRateLimiter(RateLimitConfig config) {
        this(config, Ticker.system());
    }

    @Override
    public Algorithm algorithm() {
        return Algorithm.SLIDING_WINDOW_LOG;
    }

    @Override
    protected ArrayDeque<Long> newState(long now) {
        return new ArrayDeque<>();
    }

    @Override
    protected RateLimitDecision acquire(ArrayDeque<Long> log, long now) {
        long windowStart = now - config.periodNanos();
        while (!log.isEmpty() && log.peekFirst() <= windowStart) {
            log.pollFirst();
        }
        if (log.size() < config.limit()) {
            log.addLast(now);
            return RateLimitDecision.allow(config.limit(), config.limit() - log.size());
        }
        return RateLimitDecision.reject(config.limit(), log.peekFirst() + config.periodNanos() - now);
    }
}
