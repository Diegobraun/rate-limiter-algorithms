package br.com.diegobraun.ratelimiter.algorithm;

import br.com.diegobraun.ratelimiter.core.ManualTicker;
import br.com.diegobraun.ratelimiter.core.RateLimitConfig;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class SlidingWindowCounterRateLimiterTest {

    private final ManualTicker ticker = new ManualTicker();
    private final SlidingWindowCounterRateLimiter limiter =
            new SlidingWindowCounterRateLimiter(RateLimitConfig.of(10, Duration.ofSeconds(1)), ticker);

    @Test
    void weightsPreviousWindowByRemainingOverlap() {
        for (int i = 0; i < 10; i++) {
            assertThat(limiter.tryAcquire("a").allowed()).isTrue();
        }
        ticker.advance(Duration.ofMillis(1300));

        int allowed = 0;
        while (limiter.tryAcquire("a").allowed()) {
            allowed++;
        }
        assertThat(allowed).isEqualTo(3);
    }

    @Test
    void retryAfterPointsToWhenEstimateDropsBelowLimit() {
        for (int i = 0; i < 10; i++) {
            limiter.tryAcquire("a");
        }
        ticker.advance(Duration.ofMillis(1000));
        var rejected = limiter.tryAcquire("a");
        assertThat(rejected.allowed()).isFalse();

        ticker.advanceNanos(rejected.retryAfter().toNanos());
        assertThat(limiter.tryAcquire("a").allowed()).isTrue();
    }

    @Test
    void forgetsHistoryAfterTwoIdleWindows() {
        for (int i = 0; i < 10; i++) {
            limiter.tryAcquire("a");
        }
        ticker.advance(Duration.ofMillis(2000));
        assertThat(limiter.tryAcquire("a").remaining()).isEqualTo(9);
    }
}
