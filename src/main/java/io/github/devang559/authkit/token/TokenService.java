package io.github.devang559.authkit.token;

import io.github.devang559.authkit.config.AuthKitConfig;
import io.github.devang559.authkit.exception.TokenException;
import io.github.devang559.authkit.jwt.JwtService;
import io.github.devang559.authkit.permission.PermissionService;
import io.github.devang559.authkit.role.RoleService;
import io.github.devang559.authkit.user.AuthKitUser;
import io.github.devang559.authkit.user.AuthKitUserDetailsService;
import io.github.devang559.authkit.user.AuthUser;
import io.github.devang559.authkit.user.AuthUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TokenService {

    private final JwtService jwtService;
    private final AuthUserService userService;
    private final AuthKitUserDetailsService userDetailsService;
    private final AuthKitConfig config;
    private final RoleService roleService;
    private final PermissionService permissionService;
    private final RefreshTokenRepository refreshTokenRepository;

    public TokenPair issueTokens(AuthUser user) {
        AuthKitUser details = userDetailsService.toAuthKitUser(user);
        Set<String> roles = roleService.getRoleNames(user);
        Set<String> permissions = resolvePermissions(user);

        String accessToken = jwtService.createAccessToken(user.getId(), roles, permissions);
        String refreshToken = jwtService.createRefreshToken(user.getId());

        Duration ttl = config.getProperties().getJwt().getRefreshTokenExpiration();
        RefreshToken token = new RefreshToken(user, hash(refreshToken), Instant.now().plus(ttl));
        refreshTokenRepository.save(token);

        long expiresInSeconds = config.getProperties().getJwt().getAccessTokenExpiration().getSeconds();
        return new TokenPair(accessToken, refreshToken, expiresInSeconds);
    }

    @Transactional
    public TokenPair refresh(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new TokenException("Refresh token is required");
        }
        UUID userId = jwtService.extractUserId(refreshToken);
        if (!jwtService.isRefreshToken(refreshToken)) {
            throw new TokenException("Provided token is not a refresh token");
        }

        RefreshToken stored = refreshTokenRepository.findByTokenHash(hash(refreshToken))
                .orElseThrow(() -> new TokenException("Refresh token not recognized"));
        if (!stored.isValid()) {
            throw new TokenException("Refresh token is no longer valid");
        }

        AuthUser user = userService.findById(userId)
                .orElseThrow(() -> new TokenException("User for refresh token not found"));

        TokenPair pair = issueTokens(user);

        stored.setRevoked(true);
        stored.setRevokedAt(Instant.now());
        stored.setReplacedById(stored.getId());
        refreshTokenRepository.save(stored);

        return pair;
    }

    @Transactional
    public void revokeRefreshToken(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            return;
        }
        refreshTokenRepository.findByTokenHash(hash(refreshToken)).ifPresent(token -> {
            token.setRevoked(true);
            token.setRevokedAt(Instant.now());
            refreshTokenRepository.save(token);
        });
    }

    @Transactional
    public void revokeAllUserRefreshTokens(UUID userId) {
        refreshTokenRepository.deleteAllByUserId(userId);
    }

    private Set<String> resolvePermissions(AuthUser user) {
        Set<String> perms = new java.util.HashSet<>();
        if (user.getRoles() != null) {
            for (io.github.devang559.authkit.role.AuthRole role : user.getRoles()) {
                perms.addAll(permissionService.getPermissionNames(role));
            }
        }
        return perms;
    }

    private String hash(String value) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(value.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new TokenException("Unable to hash token", e);
        }
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
