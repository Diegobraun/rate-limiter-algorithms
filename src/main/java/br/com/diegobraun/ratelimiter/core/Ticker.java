package br.com.diegobraun.ratelimiter.core;

@FunctionalInterface
public interface Ticker {

    long nanoTime();

    static Ticker system() {
        return System::nanoTime;
    }
}
