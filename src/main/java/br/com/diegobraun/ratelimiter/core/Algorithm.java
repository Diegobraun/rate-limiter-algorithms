package br.com.diegobraun.ratelimiter.core;

import br.com.diegobraun.ratelimiter.algorithm.FixedWindowRateLimiter;
import br.com.diegobraun.ratelimiter.algorithm.GcraRateLimiter;
import br.com.diegobraun.ratelimiter.algorithm.LeakyBucketRateLimiter;
import br.com.diegobraun.ratelimiter.algorithm.SlidingWindowCounterRateLimiter;
import br.com.diegobraun.ratelimiter.algorithm.SlidingWindowLogRateLimiter;
import br.com.diegobraun.ratelimiter.algorithm.TokenBucketRateLimiter;

import java.util.Arrays;
import java.util.function.BiFunction;

public enum Algorithm {

    FIXED_WINDOW("fixed-window", "Fixed Window Counter",
            "Conta requisições em janelas fixas alinhadas ao relógio. Simples e barato, mas permite até 2x o limite na virada da janela.",
            FixedWindowRateLimiter::new),
    SLIDING_WINDOW_LOG("sliding-window-log", "Sliding Window Log",
            "Guarda o timestamp de cada requisição aceita e conta as que estão dentro da janela deslizante. Preciso, porém usa memória O(limite) por chave.",
            SlidingWindowLogRateLimiter::new),
    SLIDING_WINDOW_COUNTER("sliding-window-counter", "Sliding Window Counter",
            "Aproxima a janela deslizante ponderando o contador da janela anterior. Memória O(1) com erro pequeno.",
            SlidingWindowCounterRateLimiter::new),
    TOKEN_BUCKET("token-bucket", "Token Bucket",
            "Balde de fichas reabastecido a taxa constante. Cada requisição consome uma ficha. Permite rajadas até a capacidade do balde.",
            TokenBucketRateLimiter::new),
    LEAKY_BUCKET("leaky-bucket", "Leaky Bucket (queue)",
            "Requisições entram numa fila de tamanho fixo e saem a taxa constante. Suaviza o tráfego atrasando requisições em vez de rejeitá-las.",
            LeakyBucketRateLimiter::new),
    GCRA("gcra", "GCRA",
            "Generic Cell Rate Algorithm. Equivalente ao token bucket, mas guarda apenas um timestamp (TAT) por chave. Ideal para Redis.",
            GcraRateLimiter::new);

    private final String slug;
    private final String displayName;
    private final String description;
    private final BiFunction<RateLimitConfig, Ticker, RateLimiter> factory;

    Algorithm(String slug, String displayName, String description,
              BiFunction<RateLimitConfig, Ticker, RateLimiter> factory) {
        this.slug = slug;
        this.displayName = displayName;
        this.description = description;
        this.factory = factory;
    }

    public String slug() {
        return slug;
    }

    public String displayName() {
        return displayName;
    }

    public String description() {
        return description;
    }

    public RateLimiter create(RateLimitConfig config, Ticker ticker) {
        return factory.apply(config, ticker);
    }

    public static Algorithm fromSlug(String slug) {
        return Arrays.stream(values())
                .filter(a -> a.slug.equalsIgnoreCase(slug) || a.name().equalsIgnoreCase(slug))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown algorithm: " + slug));
    }
}
