package io.github.devang559.authkit.exception;

public abstract class AuthKitException extends RuntimeException {

    public AuthKitException(String message) {
        super(message);
    }

    public AuthKitException(String message, Throwable cause) {
        super(message, cause);
    }

    public String getErrorCode() {
        return "AUTHKIT_ERROR";
    }

    public int getHttpStatus() {
        return 500;
    }
}
