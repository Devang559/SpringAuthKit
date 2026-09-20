package io.github.devang559.authkit.exception;

public class SchemaMismatchException extends AuthKitException {

    public SchemaMismatchException(String message) {
        super(message);
    }

    @Override
    public String getErrorCode() {
        return "SCHEMA_MISMATCH";
    }

    @Override
    public int getHttpStatus() {
        return 409;
    }
}
