package br.com.diegobraun.ratelimiter.algorithm;

import br.com.diegobraun.ratelimiter.core.Algorithm;
import br.com.diegobraun.ratelimiter.core.KeyedStateRateLimiter;
import br.com.diegobraun.ratelimiter.core.RateLimitConfig;
import br.com.diegobraun.ratelimiter.core.RateLimitDecision;
import br.com.diegobraun.ratelimiter.core.Ticker;

public final class LeakyBucketRateLimiter extends KeyedStateRateLimiter<LeakyBucketRateLimiter.Queue> {

    public LeakyBucketRateLimiter(RateLimitConfig config, Ticker ticker) {
        super(config, ticker);
    }

    public LeakyBucketRateLimiter(RateLimitConfig config) {
        this(config, Ticker.system());
    }

    @Override
    public Algorithm algorithm() {
        return Algorithm.LEAKY_BUCKET;
    }

    @Override
    protected Queue newState(long now) {
        return new Queue(now);
    }

    @Override
    protected RateLimitDecision acquire(Queue queue, long now) {
        long capacity = config.limit();
        long interval = config.emissionIntervalNanos();

        long nextSlot = Math.max(queue.nextFreeSlot, now);
        long waiting = Math.ceilDiv(nextSlot - now, interval);

        if (waiting >= capacity) {
            return RateLimitDecision.reject(capacity, nextSlot - (capacity - 1) * interval - now);
        }

        queue.nextFreeSlot = nextSlot + interval;
        return RateLimitDecision.allowAfter(capacity, capacity - waiting - 1, nextSlot - now);
    }

    static final class Queue {
        private long nextFreeSlot;

        private Queue(long now) {
            this.nextFreeSlot = now;
        }
    }
}
