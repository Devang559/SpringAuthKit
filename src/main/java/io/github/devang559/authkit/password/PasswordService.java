package io.github.devang559.authkit.password;

import io.github.devang559.authkit.config.AuthKitConfig;
import io.github.devang559.authkit.exception.InvalidPasswordException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PasswordService {

    private final PasswordEncoder passwordEncoder;
    private final AuthKitConfig config;

    public String hash(String raw) {
        Assert.hasText(raw, "Password must not be empty");
        return passwordEncoder.encode(raw);
    }

    public boolean matches(String raw, String hashed) {
        if (raw == null || hashed == null) {
            return false;
        }
        try {
            return passwordEncoder.matches(raw, hashed);
        } catch (Exception e) {
            return false;
        }
    }

    public void validate(String raw) {
        List<String> errors = PasswordPolicyValidator.validate(raw, config);
        if (!errors.isEmpty()) {
            throw new InvalidPasswordException("Password does not meet policy: " + String.join("; ", errors));
        }
    }
}
