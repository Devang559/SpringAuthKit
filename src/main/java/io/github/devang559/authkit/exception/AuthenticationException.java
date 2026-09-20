package io.github.devang559.authkit.exception;

public class AuthenticationException extends AuthKitException {

    public AuthenticationException(String message) {
        super(message);
    }

    public AuthenticationException(String message, Throwable cause) {
        super(message, cause);
    }

    @Override
    public String getErrorCode() {
        return "AUTHENTICATION_ERROR";
    }

    @Override
    public int getHttpStatus() {
        return 401;
    }
}
