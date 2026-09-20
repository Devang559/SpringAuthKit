package io.github.devang559.authkit.otp;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.Duration;

@Getter
@AllArgsConstructor
public class OtpPolicy {
    private final int length;
    private final Duration expiration;
    private final int maxAttempts;
}
