package br.com.diegobraun.ratelimiter.algorithm;

import br.com.diegobraun.ratelimiter.core.ManualTicker;
import br.com.diegobraun.ratelimiter.core.RateLimitConfig;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class GcraRateLimiterTest {

    private final ManualTicker ticker = new ManualTicker();
    private final GcraRateLimiter limiter =
            new GcraRateLimiter(RateLimitConfig.of(4, Duration.ofSeconds(1)), ticker);

    @Test
    void behavesLikeTokenBucketWithSingleTimestamp() {
        for (int i = 3; i >= 0; i--) {
            var decision = limiter.tryAcquire("a");
            assertThat(decision.allowed()).isTrue();
            assertThat(decision.remaining()).isEqualTo(i);
        }
        var rejected = limiter.tryAcquire("a");
        assertThat(rejected.allowed()).isFalse();
        assertThat(rejected.retryAfter()).isEqualTo(Duration.ofMillis(250));

        ticker.advance(Duration.ofMillis(250));
        assertThat(limiter.tryAcquire("a").allowed()).isTrue();
        assertThat(limiter.tryAcquire("a").allowed()).isFalse();
    }
}
