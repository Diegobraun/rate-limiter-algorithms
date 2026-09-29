package br.com.diegobraun.ratelimiter.simulation;

import br.com.diegobraun.ratelimiter.core.Algorithm;
import br.com.diegobraun.ratelimiter.core.ManualTicker;
import br.com.diegobraun.ratelimiter.core.RateLimitConfig;
import br.com.diegobraun.ratelimiter.core.RateLimitDecision;
import br.com.diegobraun.ratelimiter.core.RateLimiter;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public final class Simulator {

    private static final String KEY = "simulated-client";
    private static final double NANOS_PER_MILLI = 1_000_000.0;

    private Simulator() {
    }

    public static SimulationResult run(Algorithm algorithm, RateLimitConfig config, TrafficPattern pattern,
                                       Duration duration, long seed) {
        ManualTicker ticker = new ManualTicker();
        RateLimiter limiter = algorithm.create(config, ticker);
        long[] arrivals = pattern.arrivals(config, duration.toNanos(), new Random(seed));

        List<SimulationEvent> events = new ArrayList<>(arrivals.length);
        List<Long> executions = new ArrayList<>();
        long totalDelay = 0;

        for (long arrival : arrivals) {
            ticker.setNanos(arrival);
            RateLimitDecision decision = limiter.tryAcquire(KEY);
            long delay = decision.delay().toNanos();
            events.add(new SimulationEvent(arrival / NANOS_PER_MILLI, decision.allowed(), delay / NANOS_PER_MILLI));
            if (decision.allowed()) {
                executions.add(arrival + delay);
                totalDelay += delay;
            }
        }

        int allowed = executions.size();
        return new SimulationResult(
                algorithm.slug(),
                algorithm.displayName(),
                pattern.slug(),
                config.limit(),
                config.period().toMillis(),
                duration.toMillis(),
                arrivals.length,
                allowed,
                arrivals.length - allowed,
                maxInAnyWindow(executions, config.periodNanos()),
                allowed == 0 ? 0 : totalDelay / NANOS_PER_MILLI / allowed,
                events);
    }

    static long maxInAnyWindow(List<Long> times, long windowNanos) {
        List<Long> sorted = times.stream().sorted().toList();
        long max = 0;
        int start = 0;
        for (int end = 0; end < sorted.size(); end++) {
            while (sorted.get(end) - sorted.get(start) >= windowNanos) {
                start++;
            }
            max = Math.max(max, end - start + 1);
        }
        return max;
    }
}
