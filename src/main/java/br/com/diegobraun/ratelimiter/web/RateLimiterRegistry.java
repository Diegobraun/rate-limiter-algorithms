package br.com.diegobraun.ratelimiter.web;

import br.com.diegobraun.ratelimiter.core.Algorithm;
import br.com.diegobraun.ratelimiter.core.RateLimitConfig;
import br.com.diegobraun.ratelimiter.core.RateLimiter;
import br.com.diegobraun.ratelimiter.core.Ticker;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.Map;

@Component
public class RateLimiterRegistry {

    private final RateLimitConfig config;
    private final Map<Algorithm, RateLimiter> limiters = new EnumMap<>(Algorithm.class);

    public RateLimiterRegistry(RateLimitProperties properties) {
        this.config = RateLimitConfig.of(properties.limit(), properties.period());
        for (Algorithm algorithm : Algorithm.values()) {
            limiters.put(algorithm, algorithm.create(config, Ticker.system()));
        }
    }

    public RateLimiter get(Algorithm algorithm) {
        return limiters.get(algorithm);
    }

    public RateLimitConfig config() {
        return config;
    }
}
