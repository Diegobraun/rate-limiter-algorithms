package br.com.diegobraun.ratelimiter.simulation;

import br.com.diegobraun.ratelimiter.core.RateLimitConfig;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

public enum TrafficPattern {

    STEADY("steady", "Tráfego constante a 2x o limite") {
        @Override
        long[] arrivals(RateLimitConfig config, long durationNanos, Random random) {
            long gap = Math.max(1, config.periodNanos() / (2 * config.limit()));
            List<Long> times = new ArrayList<>();
            for (long t = 0; t < durationNanos; t += gap) {
                times.add(t);
            }
            return toArray(times);
        }
    },
    BURST("burst", "Rajadas de 2x o limite a cada 2 períodos") {
        @Override
        long[] arrivals(RateLimitConfig config, long durationNanos, Random random) {
            List<Long> times = new ArrayList<>();
            for (long start = 0; start < durationNanos; start += 2 * config.periodNanos()) {
                for (int i = 0; i < 2 * config.limit(); i++) {
                    times.add(start);
                }
            }
            return toArray(times);
        }
    },
    WINDOW_BOUNDARY("window-boundary", "Limite no fim de uma janela + limite no início da próxima") {
        @Override
        long[] arrivals(RateLimitConfig config, long durationNanos, Random random) {
            long period = config.periodNanos();
            long spread = period / 10;
            long gap = Math.max(1, spread / config.limit());
            List<Long> times = new ArrayList<>();
            for (long boundary = period; boundary < durationNanos; boundary += 2 * period) {
                for (int i = 0; i < config.limit(); i++) {
                    times.add(boundary - spread + i * gap);
                }
                for (int i = 0; i < config.limit(); i++) {
                    times.add(boundary + i * gap);
                }
            }
            return toArray(times);
        }
    },
    RANDOM("random", "Chegadas de Poisson a 1.5x o limite") {
        @Override
        long[] arrivals(RateLimitConfig config, long durationNanos, Random random) {
            double meanGap = config.periodNanos() / (1.5 * config.limit());
            List<Long> times = new ArrayList<>();
            double t = 0;
            while (true) {
                t += -Math.log(1.0 - random.nextDouble()) * meanGap;
                if (t >= durationNanos) {
                    break;
                }
                times.add((long) t);
            }
            return toArray(times);
        }
    };

    private final String slug;
    private final String description;

    TrafficPattern(String slug, String description) {
        this.slug = slug;
        this.description = description;
    }

    abstract long[] arrivals(RateLimitConfig config, long durationNanos, Random random);

    public String slug() {
        return slug;
    }

    public String description() {
        return description;
    }

    public static TrafficPattern fromSlug(String slug) {
        return Arrays.stream(values())
                .filter(p -> p.slug.equalsIgnoreCase(slug) || p.name().equalsIgnoreCase(slug))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown traffic pattern: " + slug));
    }

    private static long[] toArray(List<Long> times) {
        return times.stream().mapToLong(Long::longValue).sorted().toArray();
    }
}
