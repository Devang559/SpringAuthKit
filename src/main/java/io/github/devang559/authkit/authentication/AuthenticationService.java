package io.github.devang559.authkit.authentication;

import io.github.devang559.authkit.authentication.PrincipalIdentifier;
import io.github.devang559.authkit.config.AuthKitConfig;
import io.github.devang559.authkit.exception.AccountLockedException;
import io.github.devang559.authkit.exception.EmailNotVerifiedException;
import io.github.devang559.authkit.exception.InvalidCredentialsException;
import io.github.devang559.authkit.password.PasswordService;
import io.github.devang559.authkit.token.TokenPair;
import io.github.devang559.authkit.token.TokenService;
import io.github.devang559.authkit.user.AuthUser;
import io.github.devang559.authkit.user.AuthUserService;
import io.github.devang559.authkit.web.dto.LoginRequest;
import io.github.devang559.authkit.web.dto.LoginResponse;
import io.github.devang559.authkit.web.dto.RefreshRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthenticationService {

    private final AuthUserService userService;
    private final TokenService tokenService;
    private final PasswordService passwordService;
    private final AuthKitConfig config;

    @Transactional
    public LoginResponse authenticate(LoginRequest request) {
        AuthUser user = userService.findByIdentifier(new PrincipalIdentifier(config.getIdentifier(), request.getIdentifier()))
                .orElseThrow(() -> new InvalidCredentialsException("Invalid credentials"));

        if (user.isLockedNow()) {
            throw new AccountLockedException("Account is locked. Try again later.");
        }
        if (config.isEmailVerificationRequired() && !user.isEmailVerified()) {
            throw new EmailNotVerifiedException("Email address is not verified");
        }
        if (!passwordService.matches(request.getPassword(), user.getPassword())) {
            registerFailedLogin(user);
            throw new InvalidCredentialsException("Invalid credentials");
        }
        clearFailedLogin(user);

        TokenPair pair = tokenService.issueTokens(user);
        return LoginResponse.builder()
                .accessToken(pair.getAccessToken())
                .refreshToken(pair.getRefreshToken())
                .expiresIn(pair.getExpiresIn())
                .tokenType("Bearer")
                .build();
    }

    @Transactional
    public LoginResponse refreshToken(RefreshRequest request) {
        TokenPair pair = tokenService.refresh(request.getRefreshToken());
        return LoginResponse.builder()
                .accessToken(pair.getAccessToken())
                .refreshToken(pair.getRefreshToken())
                .expiresIn(pair.getExpiresIn())
                .tokenType("Bearer")
                .build();
    }

    @Transactional
    public void logout(RefreshRequest request) {
        tokenService.revokeRefreshToken(request.getRefreshToken());
    }

    private void registerFailedLogin(AuthUser user) {
        if (!config.isAccountLockoutEnabled()) {
            return;
        }
        int attempts = userService.incrementFailedLoginAttempts(user.getId());
        int max = config.getProperties().getSecurity().getMaxFailedAttempts();
        if (attempts >= max) {
            userService.temporaryLock(user.getId(), config.getProperties().getSecurity().getLockoutDuration());
        }
    }

    private void clearFailedLogin(AuthUser user) {
        if (config.isAccountLockoutEnabled()) {
            userService.resetFailedLoginAttempts(user.getId());
        }
    }
}
