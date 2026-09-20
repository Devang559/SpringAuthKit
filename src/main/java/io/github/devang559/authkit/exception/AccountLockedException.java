package io.github.devang559.authkit.exception;

public class AccountLockedException extends AuthKitException {

    public AccountLockedException(String message) {
        super(message);
    }

    @Override
    public String getErrorCode() {
        return "ACCOUNT_LOCKED";
    }

    @Override
    public int getHttpStatus() {
        return 403;
    }
}
