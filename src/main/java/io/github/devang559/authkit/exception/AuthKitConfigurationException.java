package io.github.devang559.authkit.exception;

public class AuthKitConfigurationException extends AuthKitException {

    public AuthKitConfigurationException(String message) {
        super(message);
    }

    public AuthKitConfigurationException(String message, Throwable cause) {
        super(message, cause);
    }

    @Override
    public String getErrorCode() {
        return "CONFIG_ERROR";
    }

    @Override
    public int getHttpStatus() {
        return 500;
    }
}
