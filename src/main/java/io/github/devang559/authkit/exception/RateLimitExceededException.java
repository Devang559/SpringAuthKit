package io.github.devang559.authkit.exception;

public class RateLimitExceededException extends AuthKitException {

    private final String retryAfterSeconds;

    public RateLimitExceededException(String message, String retryAfterSeconds) {
        super(message);
        this.retryAfterSeconds = retryAfterSeconds;
    }

    @Override
    public String getErrorCode() {
        return "RATE_LIMIT_EXCEEDED";
    }

    @Override
    public int getHttpStatus() {
        return 429;
    }

    public String getRetryAfterSeconds() {
        return retryAfterSeconds;
    }
}
