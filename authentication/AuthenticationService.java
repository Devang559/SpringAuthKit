package io.github.devang559.authkit.authentication;

import io.github.devang559.authkit.audit.AuditEvent;
import io.github.devang559.authkit.audit.AuditEventType;
import io.github.devang559.authkit.audit.AuditService;
import io.github.devang559.authkit.config.AuthKitConfig;
import io.github.devang559.authkit.exception.*;
import io.github.devang559.authkit.lifecycle.AccountStatus;
import io.github.devang559.authkit.password.PasswordService;
import io.github.devang559.authkit.ratelimit.RateLimitPolicy;
import io.github.devang559.authkit.ratelimit.RateLimiter;
import io.github.devang559.authkit.token.TokenPair;
import io.github.devang559.authkit.token.TokenService;
import io.github.devang559.authkit.user.AuthUser;
import io.github.devang559.authkit.user.AuthUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class AuthenticationService {

    private final AuthKitConfig config;
    private final AuthUserService userService;
    private final PasswordService passwordService;
    private final TokenService tokenService;
    private final RateLimiter rateLimiter;
    private final AuditService auditService;

    public TokenPair login(String identifierValue, String password) {
        RateLimitPolicy loginPolicy = new RateLimitPolicy(
                config.getProperties().getRateLimit().getLogin().getMaxAttempts(),
                config.getProperties().getRateLimit().getLogin().getWindow());
        if (config.isRateLimitEnabled()
                && !rateLimiter.tryAcquire("login:" + identifierValue, loginPolicy)) {
            throw new RateLimitExceededException("Too many login attempts. Try again later.",
                    String.valueOf(loginPolicy.window().getSeconds()));
        }

        PrincipalIdentifier identifier = new PrincipalIdentifier(config.getIdentifier(), identifierValue);
        AuthUser user = userService.findByIdentifier(identifier)
                .orElseThrow(() -> new InvalidCredentialsException("Invalid authentication credentials"));

        checkAccountState(user);

        if (!passwordService.matches(password, user.getPassword())) {
            int attempts = userService.incrementFailedLoginAttempts(user.getId());
            auditService.publish(new AuditEvent(
                    AuditEventType.LOGIN_FAILED, user.getId(), identifierValue, false, null));
            int max = config.getProperties().getSecurity().getMaxFailedAttempts();
            if (config.isAccountLockoutEnabled() && attempts >= max) {
                Duration lockout = config.getProperties().getSecurity().getLockoutDuration();
                userService.temporaryLock(user.getId(), lockout);
                throw new AccountLockedException("Account temporarily locked due to repeated failures");
            }
            throw new InvalidCredentialsException("Invalid authentication credentials");
        }

        userService.clearLockout(user.getId());
        auditService.publish(new AuditEvent(
                AuditEventType.LOGIN_SUCCESS, user.getId(), identifierValue, true, null));
        return tokenService.issueTokens(user);
    }

    private void checkAccountState(AuthUser user) {
        if (user.getAccountStatus() == AccountStatus.DISABLED) {
            throw new AccountDisabledException("Account is disabled");
        }
        if (user.getAccountStatus() == AccountStatus.DELETED) {
            throw new AuthenticationException("Account is unavailable");
        }
        if (user.isLockedNow()) {
            throw new AccountLockedException("Account is locked");
        }
        if (config.isEmailVerificationRequired() && !user.isEmailVerified()) {
            throw new EmailNotVerifiedException("Email verification required before login");
        }
    }

    public TokenPair refresh(String refreshToken) {
        TokenPair pair = tokenService.refresh(refreshToken);
        auditService.publish(new AuditEvent(AuditEventType.LOGIN_SUCCESS, null, null, true, "refresh"));
        return pair;
    }
}
