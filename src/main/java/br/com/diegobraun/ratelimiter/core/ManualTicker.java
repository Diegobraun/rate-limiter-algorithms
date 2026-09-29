package br.com.diegobraun.ratelimiter.core;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicLong;

public final class ManualTicker implements Ticker {

    private final AtomicLong nanos = new AtomicLong();

    @Override
    public long nanoTime() {
        return nanos.get();
    }

    public void advance(Duration duration) {
        nanos.addAndGet(duration.toNanos());
    }

    public void advanceNanos(long delta) {
        nanos.addAndGet(delta);
    }

    public void setNanos(long value) {
        nanos.set(value);
    }
}
