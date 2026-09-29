package br.com.diegobraun.ratelimiter.algorithm;

import br.com.diegobraun.ratelimiter.core.Algorithm;
import br.com.diegobraun.ratelimiter.core.KeyedStateRateLimiter;
import br.com.diegobraun.ratelimiter.core.RateLimitConfig;
import br.com.diegobraun.ratelimiter.core.RateLimitDecision;
import br.com.diegobraun.ratelimiter.core.Ticker;

public final class TokenBucketRateLimiter extends KeyedStateRateLimiter<TokenBucketRateLimiter.Bucket> {

    public TokenBucketRateLimiter(RateLimitConfig config, Ticker ticker) {
        super(config, ticker);
    }

    public TokenBucketRateLimiter(RateLimitConfig config) {
        this(config, Ticker.system());
    }

    @Override
    public Algorithm algorithm() {
        return Algorithm.TOKEN_BUCKET;
    }

    @Override
    protected Bucket newState(long now) {
        return new Bucket(config.limit(), now);
    }

    @Override
    protected RateLimitDecision acquire(Bucket bucket, long now) {
        long capacity = config.limit();
        double rate = config.ratePerNano();

        long elapsed = Math.max(0, now - bucket.lastRefill);
        bucket.tokens = Math.min(capacity, bucket.tokens + elapsed * rate);
        bucket.lastRefill = now;

        if (bucket.tokens >= 1.0) {
            bucket.tokens -= 1.0;
            return RateLimitDecision.allow(capacity, (long) Math.floor(bucket.tokens));
        }
        return RateLimitDecision.reject(capacity, (long) Math.ceil((1.0 - bucket.tokens) / rate));
    }

    static final class Bucket {
        private double tokens;
        private long lastRefill;

        private Bucket(double tokens, long lastRefill) {
            this.tokens = tokens;
            this.lastRefill = lastRefill;
        }
    }
}
