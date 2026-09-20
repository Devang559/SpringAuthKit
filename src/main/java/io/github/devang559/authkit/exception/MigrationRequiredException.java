package io.github.devang559.authkit.exception;

public class MigrationRequiredException extends AuthKitException {

    public MigrationRequiredException(String message) {
        super(message);
    }

    @Override
    public String getErrorCode() {
        return "MIGRATION_REQUIRED";
    }

    @Override
    public int getHttpStatus() {
        return 409;
    }
}
