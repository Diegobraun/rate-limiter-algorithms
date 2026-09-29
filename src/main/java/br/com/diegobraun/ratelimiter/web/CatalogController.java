package br.com.diegobraun.ratelimiter.web;

import br.com.diegobraun.ratelimiter.core.Algorithm;
import br.com.diegobraun.ratelimiter.simulation.TrafficPattern;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/api")
public class CatalogController {

    public record AlgorithmInfo(String slug, String name, String description) {
    }

    public record PatternInfo(String slug, String description) {
    }

    @GetMapping("/algorithms")
    public List<AlgorithmInfo> algorithms() {
        return Arrays.stream(Algorithm.values())
                .map(a -> new AlgorithmInfo(a.slug(), a.displayName(), a.description()))
                .toList();
    }

    @GetMapping("/patterns")
    public List<PatternInfo> patterns() {
        return Arrays.stream(TrafficPattern.values())
                .map(p -> new PatternInfo(p.slug(), p.description()))
                .toList();
    }
}
