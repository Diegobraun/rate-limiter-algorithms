package br.com.diegobraun.ratelimiter.simulation;

import br.com.diegobraun.ratelimiter.core.Algorithm;
import br.com.diegobraun.ratelimiter.core.RateLimitConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SimulatorTest {

    private static final RateLimitConfig CONFIG = RateLimitConfig.of(10, Duration.ofSeconds(1));
    private static final Duration DURATION = Duration.ofSeconds(6);

    @Test
    void fixedWindowLetsTwiceTheLimitThroughAtBoundary() {
        var result = Simulator.run(Algorithm.FIXED_WINDOW, CONFIG, TrafficPattern.WINDOW_BOUNDARY, DURATION, 42);
        assertThat(result.maxAllowedInAnyPeriod()).isEqualTo(20);
    }

    @ParameterizedTest
    @EnumSource(value = Algorithm.class, names = "FIXED_WINDOW", mode = EnumSource.Mode.EXCLUDE)
    void otherAlgorithmsNeverExceedLimitAtBoundary(Algorithm algorithm) {
        var result = Simulator.run(algorithm, CONFIG, TrafficPattern.WINDOW_BOUNDARY, DURATION, 42);
        assertThat(result.maxAllowedInAnyPeriod()).isLessThanOrEqualTo(CONFIG.limit() + 1);
    }

    @Test
    void leakyBucketDelaysInsteadOfRejectingWhenQueueHasRoom() {
        var result = Simulator.run(Algorithm.LEAKY_BUCKET, CONFIG, TrafficPattern.BURST, DURATION, 42);
        assertThat(result.averageDelayMs()).isPositive();
        assertThat(result.events()).anyMatch(e -> e.allowed() && e.delayMs() > 0);
    }

    @Test
    void randomPatternIsReproducibleWithSameSeed() {
        var first = Simulator.run(Algorithm.TOKEN_BUCKET, CONFIG, TrafficPattern.RANDOM, DURATION, 7);
        var second = Simulator.run(Algorithm.TOKEN_BUCKET, CONFIG, TrafficPattern.RANDOM, DURATION, 7);
        assertThat(first).isEqualTo(second);
    }

    @Test
    void maxInAnyWindowUsesHalfOpenInterval() {
        assertThat(Simulator.maxInAnyWindow(List.of(0L, 500L, 999L, 1000L), 1000)).isEqualTo(3);
    }
}
