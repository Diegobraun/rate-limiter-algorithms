package br.com.diegobraun.ratelimiter.algorithm;

import br.com.diegobraun.ratelimiter.core.ManualTicker;
import br.com.diegobraun.ratelimiter.core.RateLimitConfig;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class FixedWindowRateLimiterTest {

    private final ManualTicker ticker = new ManualTicker();
    private final FixedWindowRateLimiter limiter =
            new FixedWindowRateLimiter(RateLimitConfig.of(3, Duration.ofSeconds(1)), ticker);

    @Test
    void resetsCounterWhenWindowChanges() {
        assertThat(limiter.tryAcquire("a").allowed()).isTrue();
        assertThat(limiter.tryAcquire("a").allowed()).isTrue();
        assertThat(limiter.tryAcquire("a").allowed()).isTrue();

        ticker.advance(Duration.ofMillis(400));
        var rejected = limiter.tryAcquire("a");
        assertThat(rejected.allowed()).isFalse();
        assertThat(rejected.retryAfter()).isEqualTo(Duration.ofMillis(600));

        ticker.advance(Duration.ofMillis(600));
        assertThat(limiter.tryAcquire("a").allowed()).isTrue();
    }

    @Test
    void allowsTwiceTheLimitAroundWindowBoundary() {
        ticker.advance(Duration.ofMillis(990));
        for (int i = 0; i < 3; i++) {
            assertThat(limiter.tryAcquire("a").allowed()).isTrue();
        }
        ticker.advance(Duration.ofMillis(20));
        for (int i = 0; i < 3; i++) {
            assertThat(limiter.tryAcquire("a").allowed()).isTrue();
        }
    }
}
