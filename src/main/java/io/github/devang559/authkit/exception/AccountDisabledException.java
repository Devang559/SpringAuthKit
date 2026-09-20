package io.github.devang559.authkit.exception;

public class AccountDisabledException extends AuthKitException {

    public AccountDisabledException(String message) {
        super(message);
    }

    @Override
    public String getErrorCode() {
        return "ACCOUNT_DISABLED";
    }

    @Override
    public int getHttpStatus() {
        return 403;
    }
}
