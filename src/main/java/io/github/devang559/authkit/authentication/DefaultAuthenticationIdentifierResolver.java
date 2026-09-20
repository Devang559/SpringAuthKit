package io.github.devang559.authkit.authentication;

import io.github.devang559.authkit.config.AuthKitConfig;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;

@RequiredArgsConstructor
public class DefaultAuthenticationIdentifierResolver implements AuthenticationIdentifierResolver {

    private final AuthKitConfig config;

    @Override
    public PrincipalIdentifier resolve(HttpServletRequest request) {
        String value = request.getParameter("identifier");
        if (!StringUtils.hasText(value)) {
            value = request.getHeader("X-Auth-Identifier");
        }
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return new PrincipalIdentifier(config.getIdentifier(), value);
    }
}
