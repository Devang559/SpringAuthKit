package io.github.devang559.authkit.security;

import io.github.devang559.authkit.exception.TokenException;
import io.github.devang559.authkit.jwt.JwtService;
import io.github.devang559.authkit.user.AuthKitUser;
import io.github.devang559.authkit.user.AuthKitUserDetailsService;
import io.github.devang559.authkit.user.AuthUser;
import io.github.devang559.authkit.user.AuthUserService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Slf4j
public class AuthKitJwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final AuthUserService userService;
    private final AuthKitUserDetailsService userDetailsService;

    public AuthKitJwtAuthenticationFilter(JwtService jwtService,
                                          AuthUserService userService,
                                          AuthKitUserDetailsService userDetailsService) {
        this.jwtService = jwtService;
        this.userService = userService;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (StringUtils.hasText(header) && header.startsWith("Bearer ")) {
            String token = header.substring(7);
            try {
                UUID userId = jwtService.extractUserId(token);
                AuthUser user = userService.findById(userId).orElse(null);
                if (user != null && user.isEnabled()
                        && user.getAccountStatus() == io.github.devang559.authkit.lifecycle.AccountStatus.ACTIVE
                        && !user.isLockedNow()) {
                    AuthKitUser details = userDetailsService.toAuthKitUser(user);
                    AuthKitAuthenticationToken authentication =
                            new AuthKitAuthenticationToken(details, details.getAuthorities());
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            } catch (TokenException e) {
                logger.debug("Invalid bearer token: {}", e.getMessage());
            }
        }
        filterChain.doFilter(request, response);
    }
}
