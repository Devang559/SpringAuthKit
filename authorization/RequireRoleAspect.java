package io.github.devang559.authkit.authorization;

import io.github.devang559.authkit.config.AuthKitConfig;
import io.github.devang559.authkit.exception.RoleConfigurationException;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Collection;

@Aspect
@Component
@RequiredArgsConstructor
public class RequireRoleAspect {

    private final AuthKitConfig config;

    @Around("@annotation(requireRole)")
    public Object enforce(ProceedingJoinPoint joinPoint, RequireRole requireRole) throws Throwable {
        if (!config.isRolesEnabled()) {
            throw new RoleConfigurationException(
                    "@RequireRole is used but roles are disabled in configuration. " +
                            "Enable roles with 'spring.authkit.roles.enabled=true' or remove the annotation.");
        }
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AccessDeniedException("Access denied: authentication required");
        }
        Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();
        String[] required = requireRole.value();
        boolean authorized = Arrays.stream(required).anyMatch(role ->
                authorities.stream().anyMatch(a -> a.getAuthority().equals("ROLE_" + role)));
        if (!authorized) {
            throw new AccessDeniedException("Access denied: missing required role(s) " + Arrays.toString(required));
        }
        return joinPoint.proceed();
    }
}
