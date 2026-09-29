package br.com.diegobraun.ratelimiter.algorithm;

import br.com.diegobraun.ratelimiter.core.Algorithm;
import br.com.diegobraun.ratelimiter.core.KeyedStateRateLimiter;
import br.com.diegobraun.ratelimiter.core.RateLimitConfig;
import br.com.diegobraun.ratelimiter.core.RateLimitDecision;
import br.com.diegobraun.ratelimiter.core.Ticker;

public final class GcraRateLimiter extends KeyedStateRateLimiter<GcraRateLimiter.TheoreticalArrival> {

    public GcraRateLimiter(RateLimitConfig config, Ticker ticker) {
        super(config, ticker);
    }

    public GcraRateLimiter(RateLimitConfig config) {
        this(config, Ticker.system());
    }

    @Override
    public Algorithm algorithm() {
        return Algorithm.GCRA;
    }

    @Override
    protected TheoreticalArrival newState(long now) {
        return new TheoreticalArrival(now);
    }

    @Override
    protected RateLimitDecision acquire(TheoreticalArrival state, long now) {
        long limit = config.limit();
        long emissionInterval = config.emissionIntervalNanos();
        long burstOffset = emissionInterval * limit;

        long tat = Math.max(state.tat, now);
        long newTat = tat + emissionInterval;
        long allowAt = newTat - burstOffset;

        if (now < allowAt) {
            return RateLimitDecision.reject(limit, allowAt - now);
        }

        state.tat = newTat;
        return RateLimitDecision.allow(limit, (now - allowAt) / emissionInterval);
    }

    static final class TheoreticalArrival {
        private long tat;

        private TheoreticalArrival(long now) {
            this.tat = now;
        }
    }
}
