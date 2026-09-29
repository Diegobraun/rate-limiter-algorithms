package br.com.diegobraun.ratelimiter.algorithm;

import br.com.diegobraun.ratelimiter.core.ManualTicker;
import br.com.diegobraun.ratelimiter.core.RateLimitConfig;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class LeakyBucketRateLimiterTest {

    private final ManualTicker ticker = new ManualTicker();
    private final LeakyBucketRateLimiter limiter =
            new LeakyBucketRateLimiter(RateLimitConfig.of(4, Duration.ofSeconds(1)), ticker);

    @Test
    void queuesBurstAndSpacesRequestsAtConstantRate() {
        assertThat(limiter.tryAcquire("a").delay()).isEqualTo(Duration.ZERO);
        assertThat(limiter.tryAcquire("a").delay()).isEqualTo(Duration.ofMillis(250));
        assertThat(limiter.tryAcquire("a").delay()).isEqualTo(Duration.ofMillis(500));
        assertThat(limiter.tryAcquire("a").delay()).isEqualTo(Duration.ofMillis(750));

        var rejected = limiter.tryAcquire("a");
        assertThat(rejected.allowed()).isFalse();
        assertThat(rejected.retryAfter()).isEqualTo(Duration.ofMillis(250));
    }

    @Test
    void drainsQueueOverTime() {
        for (int i = 0; i < 4; i++) {
            limiter.tryAcquire("a");
        }
        ticker.advance(Duration.ofMillis(500));
        var decision = limiter.tryAcquire("a");
        assertThat(decision.allowed()).isTrue();
        assertThat(decision.delay()).isEqualTo(Duration.ofMillis(500));
    }
}
