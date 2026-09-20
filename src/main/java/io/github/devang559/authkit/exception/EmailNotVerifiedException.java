package io.github.devang559.authkit.exception;

public class EmailNotVerifiedException extends AuthKitException {

    public EmailNotVerifiedException(String message) {
        super(message);
    }

    @Override
    public String getErrorCode() {
        return "EMAIL_NOT_VERIFIED";
    }

    @Override
    public int getHttpStatus() {
        return 401;
    }
}
