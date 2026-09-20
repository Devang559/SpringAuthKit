package io.github.devang559.authkit.jwt;

import io.github.devang559.authkit.config.AuthKitConfig;
import io.github.devang559.authkit.exception.TokenException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.*;

@Service
@RequiredArgsConstructor
public class JwtService {

    private final AuthKitConfig config;

    public String createAccessToken(UUID userId, Collection<String> roles, Collection<String> permissions) {
        SecretKey key = secretKey();
        Instant now = Instant.now();
        Date nowDate = Date.from(now);
        Date expDate = Date.from(now.plus(config.getProperties().getJwt().getAccessTokenExpiration()));

        Map<String, Object> claims = new HashMap<>();
        claims.put("type", "access");
        claims.put("roles", roles == null ? Collections.emptyList() : roles);
        claims.put("permissions", permissions == null ? Collections.emptyList() : permissions);

        return Jwts.builder()
                .claims(claims)
                .id(UUID.randomUUID().toString())
                .subject(userId.toString())
                .issuer(config.getProperties().getJwt().getIssuer())
                .issuedAt(nowDate)
                .expiration(expDate)
                .signWith(key)
                .compact();
    }

    public String createRefreshToken(UUID userId) {
        SecretKey key = secretKey();
        Instant now = Instant.now();
        Duration ttl = config.getProperties().getJwt().getRefreshTokenExpiration();
        Date nowDate = Date.from(now);
        Date expDate = Date.from(now.plus(ttl));

        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .subject(userId.toString())
                .issuer(config.getProperties().getJwt().getIssuer())
                .claim("type", "refresh")
                .issuedAt(nowDate)
                .expiration(expDate)
                .signWith(key)
                .compact();
    }

    public Claims parseClaims(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(secretKey())
                    .requireIssuer(config.getProperties().getJwt().getIssuer())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (JwtException e) {
            throw new TokenException("Invalid or expired token", e);
        }
    }

    public UUID extractUserId(String token) {
        Claims claims = parseClaims(token);
        try {
            return UUID.fromString(claims.getSubject());
        } catch (IllegalArgumentException e) {
            throw new TokenException("Token subject is not a valid user id");
        }
    }

    public boolean isRefreshToken(String token) {
        Claims claims = parseClaims(token);
        return "refresh".equals(claims.get("type"));
    }

    public String getJwtId(String token) {
        return parseClaims(token).getId();
    }

    private SecretKey secretKey() {
        String secret = config.getProperties().getJwt().getSecret();
        return new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    }
}
