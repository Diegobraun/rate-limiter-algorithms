package br.com.diegobraun.ratelimiter.simulation;

public record SimulationEvent(double arrivalMs, boolean allowed, double delayMs) {
}
