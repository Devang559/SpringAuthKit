package io.github.devang559.authkit.jwt;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class JwtProperties {
    private final long accessTokenExpiresInSeconds;
    private final long refreshTokenExpiresInSeconds;
}
