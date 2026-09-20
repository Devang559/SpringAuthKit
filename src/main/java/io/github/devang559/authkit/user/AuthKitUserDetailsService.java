package io.github.devang559.authkit.user;

import io.github.devang559.authkit.authentication.PrincipalIdentifier;
import io.github.devang559.authkit.config.AuthKitConfig;
import io.github.devang559.authkit.permission.PermissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthKitUserDetailsService implements UserDetailsService {

    private final AuthUserService userService;
    private final AuthKitConfig config;
    private final PermissionService permissionService;

    @Override
    public UserDetails loadUserByUsername(String identifierValue) throws UsernameNotFoundException {
        PrincipalIdentifier identifier = new PrincipalIdentifier(config.getIdentifier(), identifierValue);
        AuthUser user = userService.findByIdentifier(identifier)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        return toAuthKitUser(user);
    }

    public AuthKitUser toAuthKitUser(AuthUser user) {
        String identifierValue = resolveIdentifierValue(user);
        Set<String> roleNames = user.getRoles().stream()
                .map(io.github.devang559.authkit.role.AuthRole::getName)
                .collect(java.util.stream.Collectors.toSet());
        Set<String> permissionNames = resolvePermissions(user);
        return new AuthKitUser(
                user.getId(),
                identifierValue,
                user.getPassword(),
                user.isEnabled(),
                user.isEmailVerified(),
                user.isPhoneVerified(),
                user.getAccountStatus(),
                roleNames,
                permissionNames,
                config
        );
    }

    private String resolveIdentifierValue(AuthUser user) {
        return switch (config.getIdentifier()) {
            case EMAIL -> user.getEmail();
            case USERNAME -> user.getUsername();
            case PHONE -> user.getPhone();
        };
    }

    private Set<String> resolvePermissions(AuthUser user) {
        if (!config.isPermissionsEnabled()) {
            return Set.of();
        }
        Set<String> perms = new java.util.HashSet<>();
        if (user.getRoles() != null) {
            for (io.github.devang559.authkit.role.AuthRole role : user.getRoles()) {
                perms.addAll(permissionService.getPermissionNames(role));
            }
        }
        return perms;
    }
}
