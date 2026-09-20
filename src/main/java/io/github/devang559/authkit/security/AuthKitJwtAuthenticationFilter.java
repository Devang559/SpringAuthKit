package io.github.devang559.authkit.security;

import io.github.devang559.authkit.jwt.JwtService;
import io.github.devang559.authkit.user.AuthKitUser;
import io.github.devang559.authkit.user.AuthKitUserDetailsService;
import io.github.devang559.authkit.user.AuthUserService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

@Slf4j
public class AuthKitJwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final AuthKitUserDetailsService userDetailsService;
    private final AuthUserService userService;

    public AuthKitJwtAuthenticationFilter(JwtService jwtService,
                                          AuthKitUserDetailsService userDetailsService,
                                          AuthUserService userService) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
        this.userService = userService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);
            authenticateIfValid(token, request);
        }
        filterChain.doFilter(request, response);
    }

    private void authenticateIfValid(String token, HttpServletRequest request) {
        try {
            UUID userId = jwtService.extractUserId(token);
            Optional<io.github.devang559.authkit.user.AuthUser> maybe = userService.findById(userId);
            if (maybe.isPresent() && SecurityContextHolder.getContext().getAuthentication() == null) {
                AuthKitUser userDetails = userDetailsService.toAuthKitUser(maybe.get());
                UsernamePasswordAuthenticationToken auth =
                        new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(auth);
            }
        } catch (Exception e) {
            log.debug("SpringAuthKit: rejected bearer token: {}", e.getMessage());
        }
    }
}
