package br.com.diegobraun.ratelimiter.algorithm;

import br.com.diegobraun.ratelimiter.core.ManualTicker;
import br.com.diegobraun.ratelimiter.core.RateLimitConfig;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class TokenBucketRateLimiterTest {

    private final ManualTicker ticker = new ManualTicker();
    private final TokenBucketRateLimiter limiter =
            new TokenBucketRateLimiter(RateLimitConfig.of(4, Duration.ofSeconds(1)), ticker);

    @Test
    void allowsBurstUpToCapacityThenRefillsGradually() {
        for (int i = 0; i < 4; i++) {
            assertThat(limiter.tryAcquire("a").allowed()).isTrue();
        }
        var rejected = limiter.tryAcquire("a");
        assertThat(rejected.allowed()).isFalse();
        assertThat(rejected.retryAfter()).isEqualTo(Duration.ofMillis(250));

        ticker.advance(Duration.ofMillis(250));
        assertThat(limiter.tryAcquire("a").allowed()).isTrue();
        assertThat(limiter.tryAcquire("a").allowed()).isFalse();
    }

    @Test
    void neverAccumulatesMoreThanCapacity() {
        ticker.advance(Duration.ofHours(1));
        int allowed = 0;
        while (limiter.tryAcquire("a").allowed()) {
            allowed++;
        }
        assertThat(allowed).isEqualTo(4);
    }
}
