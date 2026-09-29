package br.com.diegobraun.ratelimiter.web;

import br.com.diegobraun.ratelimiter.core.Algorithm;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;

@RestController
@RequestMapping("/api/limited")
public class LimitedController {

    @GetMapping("/{algorithm}")
    public Map<String, Object> hit(@PathVariable String algorithm) {
        return Map.of(
                "message", "Request accepted",
                "algorithm", Algorithm.fromSlug(algorithm).slug(),
                "timestamp", Instant.now().toString());
    }
}
