package io.github.devang559.authkit.exception;

public class OtpExpiredException extends AuthKitException {

    public OtpExpiredException(String message) {
        super(message);
    }

    @Override
    public String getErrorCode() {
        return "OTP_EXPIRED";
    }

    @Override
    public int getHttpStatus() {
        return 400;
    }
}
