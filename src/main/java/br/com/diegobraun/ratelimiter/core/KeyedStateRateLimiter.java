package br.com.diegobraun.ratelimiter.core;

import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public abstract class KeyedStateRateLimiter<S> implements RateLimiter {

    protected final RateLimitConfig config;
    private final Ticker ticker;
    private final ConcurrentMap<String, S> states = new ConcurrentHashMap<>();

    protected KeyedStateRateLimiter(RateLimitConfig config, Ticker ticker) {
        this.config = Objects.requireNonNull(config, "config");
        this.ticker = Objects.requireNonNull(ticker, "ticker");
    }

    @Override
    public final RateLimitDecision tryAcquire(String key) {
        Objects.requireNonNull(key, "key");
        S state = states.computeIfAbsent(key, k -> newState(ticker.nanoTime()));
        synchronized (state) {
            return acquire(state, ticker.nanoTime());
        }
    }

    protected abstract S newState(long now);

    protected abstract RateLimitDecision acquire(S state, long now);
}
