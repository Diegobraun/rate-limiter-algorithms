package br.com.diegobraun.ratelimiter.algorithm;

import br.com.diegobraun.ratelimiter.core.ManualTicker;
import br.com.diegobraun.ratelimiter.core.RateLimitConfig;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class SlidingWindowLogRateLimiterTest {

    private final ManualTicker ticker = new ManualTicker();
    private final SlidingWindowLogRateLimiter limiter =
            new SlidingWindowLogRateLimiter(RateLimitConfig.of(3, Duration.ofSeconds(1)), ticker);

    @Test
    void doesNotAllowBurstAcrossWindowBoundary() {
        ticker.advance(Duration.ofMillis(990));
        for (int i = 0; i < 3; i++) {
            assertThat(limiter.tryAcquire("a").allowed()).isTrue();
        }
        ticker.advance(Duration.ofMillis(20));
        var rejected = limiter.tryAcquire("a");
        assertThat(rejected.allowed()).isFalse();
        assertThat(rejected.retryAfter()).isEqualTo(Duration.ofMillis(980));
    }

    @Test
    void releasesSlotsAsOldRequestsLeaveTheWindow() {
        limiter.tryAcquire("a");
        ticker.advance(Duration.ofMillis(300));
        limiter.tryAcquire("a");
        limiter.tryAcquire("a");
        assertThat(limiter.tryAcquire("a").allowed()).isFalse();

        ticker.advance(Duration.ofMillis(700));
        var decision = limiter.tryAcquire("a");
        assertThat(decision.allowed()).isTrue();
        assertThat(decision.remaining()).isZero();
    }
}
