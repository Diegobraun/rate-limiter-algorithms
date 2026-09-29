package br.com.diegobraun.ratelimiter.algorithm;

import br.com.diegobraun.ratelimiter.core.Algorithm;
import br.com.diegobraun.ratelimiter.core.ManualTicker;
import br.com.diegobraun.ratelimiter.core.RateLimitConfig;
import br.com.diegobraun.ratelimiter.core.RateLimitDecision;
import br.com.diegobraun.ratelimiter.core.RateLimiter;
import br.com.diegobraun.ratelimiter.core.Ticker;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class RateLimiterContractTest {

    private static final RateLimitConfig CONFIG = RateLimitConfig.of(5, Duration.ofSeconds(1));

    @ParameterizedTest
    @EnumSource(Algorithm.class)
    void allowsExactlyLimitRequestsInAnInstantaneousBurst(Algorithm algorithm) {
        RateLimiter limiter = algorithm.create(CONFIG, new ManualTicker());
        int allowed = 0;
        for (int i = 0; i < 20; i++) {
            if (limiter.tryAcquire("client").allowed()) {
                allowed++;
            }
        }
        assertThat(allowed).isEqualTo(5);
    }

    @ParameterizedTest
    @EnumSource(Algorithm.class)
    void isolatesKeys(Algorithm algorithm) {
        RateLimiter limiter = algorithm.create(CONFIG, new ManualTicker());
        for (int i = 0; i < 5; i++) {
            limiter.tryAcquire("alice");
        }
        assertThat(limiter.tryAcquire("alice").allowed()).isFalse();
        assertThat(limiter.tryAcquire("bob").allowed()).isTrue();
    }

    @ParameterizedTest
    @EnumSource(Algorithm.class)
    void rejectedDecisionReportsPositiveRetryAfterThatActuallyWorks(Algorithm algorithm) {
        ManualTicker ticker = new ManualTicker();
        ticker.advance(Duration.ofMillis(123));
        RateLimiter limiter = algorithm.create(CONFIG, ticker);
        RateLimitDecision decision;
        do {
            decision = limiter.tryAcquire("client");
        } while (decision.allowed());

        assertThat(decision.retryAfter()).isPositive();
        assertThat(decision.remaining()).isZero();

        ticker.advanceNanos(decision.retryAfter().toNanos());
        assertThat(limiter.tryAcquire("client").allowed()).isTrue();
    }

    @ParameterizedTest
    @EnumSource(Algorithm.class)
    void isThreadSafeUnderContention(Algorithm algorithm) throws InterruptedException {
        RateLimiter limiter = algorithm.create(RateLimitConfig.of(100, Duration.ofHours(1)), Ticker.system());
        AtomicInteger allowed = new AtomicInteger();
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(1000);

        try (var executor = Executors.newFixedThreadPool(16)) {
            for (int i = 0; i < 1000; i++) {
                executor.submit(() -> {
                    try {
                        start.await();
                        if (limiter.tryAcquire("shared").allowed()) {
                            allowed.incrementAndGet();
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } finally {
                        done.countDown();
                    }
                });
            }
            start.countDown();
            done.await();
        }

        assertThat(allowed.get()).isEqualTo(100);
    }
}
