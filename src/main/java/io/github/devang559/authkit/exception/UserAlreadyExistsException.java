package io.github.devang559.authkit.exception;

public class UserAlreadyExistsException extends AuthKitException {

    public UserAlreadyExistsException(String message) {
        super(message);
    }

    @Override
    public String getErrorCode() {
        return "USER_ALREADY_EXISTS";
    }

    @Override
    public int getHttpStatus() {
        return 409;
    }
}
