package io.github.devang559.authkit.exception;

public class InvalidPasswordException extends AuthKitException {

    public InvalidPasswordException(String message) {
        super(message);
    }

    @Override
    public String getErrorCode() {
        return "INVALID_PASSWORD";
    }

    @Override
    public int getHttpStatus() {
        return 400;
    }
}
