package io.github.devang559.authkit.exception;

public class InvalidOtpException extends AuthKitException {

    public InvalidOtpException(String message) {
        super(message);
    }

    @Override
    public String getErrorCode() {
        return "INVALID_OTP";
    }

    @Override
    public int getHttpStatus() {
        return 400;
    }
}
