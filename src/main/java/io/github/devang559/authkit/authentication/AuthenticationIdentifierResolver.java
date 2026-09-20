package io.github.devang559.authkit.authentication;

import jakarta.servlet.http.HttpServletRequest;

public interface AuthenticationIdentifierResolver {

    PrincipalIdentifier resolve(HttpServletRequest request);
}
