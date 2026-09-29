package br.com.diegobraun.ratelimiter.simulation;

import java.util.List;

public record SimulationResult(
        String algorithm,
        String displayName,
        String pattern,
        long limit,
        long periodMs,
        long durationMs,
        int total,
        int allowed,
        int rejected,
        long maxAllowedInAnyPeriod,
        double averageDelayMs,
        List<SimulationEvent> events) {
}
