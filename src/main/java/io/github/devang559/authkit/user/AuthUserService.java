package io.github.devang559.authkit.user;

import io.github.devang559.authkit.authentication.PrincipalIdentifier;
import io.github.devang559.authkit.config.AuthKitConfig;
import io.github.devang559.authkit.exception.UserAlreadyExistsException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthUserService {

    private final AuthUserRepository userRepository;
    private final AuthKitConfig config;

    public Optional<AuthUser> findById(UUID id) {
        return userRepository.findById(id);
    }

    public Optional<AuthUser> findByIdentifier(PrincipalIdentifier identifier) {
        return switch (identifier.getType()) {
            case EMAIL -> userRepository.findByEmail(identifier.getValue());
            case USERNAME -> userRepository.findByUsername(identifier.getValue());
            case PHONE -> userRepository.findByPhone(identifier.getValue());
        };
    }

    public Optional<AuthUser> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    public Optional<AuthUser> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    public Optional<AuthUser> findByPhone(String phone) {
        return userRepository.findByPhone(phone);
    }

    public boolean isFieldTaken(String field, String value) {
        if (value == null || value.isBlank()) {
            return false;
        }
        return switch (field) {
            case "email" -> userRepository.findByEmail(value).isPresent();
            case "username" -> userRepository.findByUsername(value).isPresent();
            case "phone" -> userRepository.findByPhone(value).isPresent();
            default -> false;
        };
    }

    @org.springframework.transaction.annotation.Transactional
    public void markEmailVerified(UUID userId) {
        findById(userId).ifPresent(u -> u.setEmailVerified(true));
    }

    @org.springframework.transaction.annotation.Transactional
    public void markPhoneVerified(UUID userId) {
        findById(userId).ifPresent(u -> u.setPhoneVerified(true));
    }

    @org.springframework.transaction.annotation.Transactional
    public void resetFailedLoginAttempts(UUID userId) {
        findById(userId).ifPresent(u -> u.setFailedLoginAttempts(0));
    }

    @org.springframework.transaction.annotation.Transactional
    public int incrementFailedLoginAttempts(UUID userId) {
        AuthUser user = findById(userId).orElse(null);
        if (user == null) {
            return 0;
        }
        user.setFailedLoginAttempts(user.getFailedLoginAttempts() + 1);
        return user.getFailedLoginAttempts();
    }

    @org.springframework.transaction.annotation.Transactional
    public void temporaryLock(UUID userId, java.time.Duration duration) {
        findById(userId).ifPresent(u -> {
            u.setAccountStatus(io.github.devang559.authkit.lifecycle.AccountStatus.LOCKED);
            u.setLockedUntil(java.time.Instant.now().plus(duration));
        });
    }

    @org.springframework.transaction.annotation.Transactional
    public void clearLockout(UUID userId) {
        findById(userId).ifPresent(u -> {
            u.setFailedLoginAttempts(0);
            u.setLockedUntil(null);
            u.setAccountStatus(io.github.devang559.authkit.lifecycle.AccountStatus.ACTIVE);
        });
    }

    public AuthUser save(AuthUser user) {
        return userRepository.save(user);
    }
}
