package io.github.devang559.authkit.ratelimit;

import java.time.Duration;

public record RateLimitPolicy(int maxAttempts, Duration window) {
}
