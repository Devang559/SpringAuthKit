package io.github.devang559.authkit.ratelimit;

public interface RateLimiter {

    boolean tryAcquire(String key, RateLimitPolicy policy);
}
