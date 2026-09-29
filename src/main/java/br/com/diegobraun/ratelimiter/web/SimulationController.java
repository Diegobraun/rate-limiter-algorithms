package br.com.diegobraun.ratelimiter.web;

import br.com.diegobraun.ratelimiter.core.Algorithm;
import br.com.diegobraun.ratelimiter.core.RateLimitConfig;
import br.com.diegobraun.ratelimiter.simulation.SimulationResult;
import br.com.diegobraun.ratelimiter.simulation.Simulator;
import br.com.diegobraun.ratelimiter.simulation.TrafficPattern;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/api/simulations")
public class SimulationController {

    @GetMapping
    public List<SimulationResult> simulate(
            @RequestParam(required = false) String algorithm,
            @RequestParam(defaultValue = "window-boundary") String pattern,
            @RequestParam(defaultValue = "10") @Min(1) @Max(200) long limit,
            @RequestParam(defaultValue = "1000") @Min(100) @Max(60000) long periodMs,
            @RequestParam(defaultValue = "6") @Min(1) @Max(20) int periods,
            @RequestParam(defaultValue = "42") long seed) {

        RateLimitConfig config = RateLimitConfig.of(limit, Duration.ofMillis(periodMs));
        TrafficPattern trafficPattern = TrafficPattern.fromSlug(pattern);
        Duration duration = Duration.ofMillis(periodMs * periods);

        List<Algorithm> algorithms = algorithm == null || algorithm.isBlank()
                ? Arrays.asList(Algorithm.values())
                : List.of(Algorithm.fromSlug(algorithm));

        return algorithms.stream()
                .map(a -> Simulator.run(a, config, trafficPattern, duration, seed))
                .toList();
    }
}
