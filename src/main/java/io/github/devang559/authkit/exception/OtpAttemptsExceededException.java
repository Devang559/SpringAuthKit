package io.github.devang559.authkit.exception;

public class OtpAttemptsExceededException extends AuthKitException {

    public OtpAttemptsExceededException(String message) {
        super(message);
    }

    @Override
    public String getErrorCode() {
        return "OTP_ATTEMPTS_EXCEEDED";
    }

    @Override
    public int getHttpStatus() {
        return 429;
    }
}
