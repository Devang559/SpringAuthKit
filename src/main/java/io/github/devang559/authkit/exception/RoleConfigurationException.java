package io.github.devang559.authkit.exception;

public class RoleConfigurationException extends AuthKitException {

    public RoleConfigurationException(String message) {
        super(message);
    }

    @Override
    public String getErrorCode() {
        return "ROLE_CONFIG_ERROR";
    }

    @Override
    public int getHttpStatus() {
        return 500;
    }
}
