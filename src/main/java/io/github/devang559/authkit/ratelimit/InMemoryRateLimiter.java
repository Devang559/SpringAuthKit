package io.github.devang559.authkit.ratelimit;

import java.time.Duration;
import java.time.Instant;
import java.util.Deque;
import java.util.concurrent.ConcurrentLinkedDeque;

public class InMemoryRateLimiter implements RateLimiter {

    private final java.util.Map<String, Deque<Instant>> buckets = new java.util.concurrent.ConcurrentHashMap<>();

    @Override
    public boolean tryAcquire(String key, RateLimitPolicy policy) {
        Instant now = Instant.now();
        Duration window = policy.window();
        Instant cutoff = now.minus(window);
        Deque<Instant> bucket = buckets.computeIfAbsent(key, k -> new ConcurrentLinkedDeque<>());

        bucket.removeIf(t -> t.isBefore(cutoff));
        if (bucket.size() >= policy.maxAttempts()) {
            return false;
        }
        bucket.addLast(now);
        return true;
    }
}
