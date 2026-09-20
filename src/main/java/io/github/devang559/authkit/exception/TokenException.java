package io.github.devang559.authkit.exception;

public class TokenException extends AuthKitException {

    public TokenException(String message) {
        super(message);
    }

    public TokenException(String message, Throwable cause) {
        super(message, cause);
    }

    @Override
    public String getErrorCode() {
        return "TOKEN_ERROR";
    }

    @Override
    public int getHttpStatus() {
        return 401;
    }
}
