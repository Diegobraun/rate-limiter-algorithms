package br.com.diegobraun.ratelimiter.web;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Validated
@ConfigurationProperties("rate-limit")
public record RateLimitProperties(@Positive long limit, @NotNull Duration period) {
}
