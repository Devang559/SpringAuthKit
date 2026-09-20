package io.github.devang559.authkit.password;

import io.github.devang559.authkit.config.AuthKitConfig;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.ArrayList;
import java.util.List;

public class PasswordPolicyValidator {

    public static List<String> validate(String raw, AuthKitConfig config) {
        List<String> errors = new ArrayList<>();
        if (raw == null || raw.isEmpty()) {
            errors.add("Password must not be empty.");
            return errors;
        }
        if (raw.length() < config.getProperties().getPassword().getMinLength()) {
            errors.add("Password must be at least " + config.getProperties().getPassword().getMinLength() + " characters long.");
        }
        if (config.getProperties().getPassword().isRequireUppercase() && raw.equals(raw.toLowerCase())) {
            errors.add("Password must contain at least one uppercase letter.");
        }
        if (config.getProperties().getPassword().isRequireLowercase() && raw.equals(raw.toUpperCase())) {
            errors.add("Password must contain at least one lowercase letter.");
        }
        if (config.getProperties().getPassword().isRequireNumber() && !raw.matches(".*\\d.*")) {
            errors.add("Password must contain at least one number.");
        }
        if (config.getProperties().getPassword().isRequireSpecialCharacter() && !raw.matches(".*[!@#$%^&*()_+\\-=`~\\[\\]{};':\"\\\\|,.<>/?].*")) {
            errors.add("Password must contain at least one special character.");
        }
        return errors;
    }

    public static boolean isValid(String raw, AuthKitConfig config) {
        return validate(raw, config).isEmpty();
    }

    public static String hash(String raw, PasswordEncoder encoder) {
        return encoder.encode(raw);
    }

    public static boolean matches(String raw, String hashed, PasswordEncoder encoder) {
        return encoder.matches(raw, hashed);
    }
}
