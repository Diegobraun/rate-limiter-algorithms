package br.com.diegobraun.ratelimiter.core;

public interface RateLimiter {

    RateLimitDecision tryAcquire(String key);

    Algorithm algorithm();
}
