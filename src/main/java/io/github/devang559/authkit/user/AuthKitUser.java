package io.github.devang559.authkit.user;

import io.github.devang559.authkit.config.AuthKitConfig;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Getter
public class AuthKitUser implements UserDetails {

    private final UUID id;
    private final String identifier;
    private final String password;
    private final boolean enabled;
    private final boolean emailVerified;
    private final boolean phoneVerified;
    private final io.github.devang559.authkit.lifecycle.AccountStatus status;
    private final Set<String> roleNames;
    private final Set<String> permissionNames;
    private final AuthKitConfig config;

    public AuthKitUser(UUID id, String identifier, String password, boolean enabled,
                       boolean emailVerified, boolean phoneVerified,
                       io.github.devang559.authkit.lifecycle.AccountStatus status,
                       Set<String> roleNames, Set<String> permissionNames, AuthKitConfig config) {
        this.id = id;
        this.identifier = identifier;
        this.password = password;
        this.enabled = enabled;
        this.emailVerified = emailVerified;
        this.phoneVerified = phoneVerified;
        this.status = status;
        this.roleNames = roleNames;
        this.permissionNames = permissionNames;
        this.config = config;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        Set<GrantedAuthority> authorities = roleNames.stream()
                .map(name -> new SimpleGrantedAuthority("ROLE_" + name))
                .collect(Collectors.toSet());
        if (config.isPermissionsEnabled() && permissionNames != null) {
            for (String p : permissionNames) {
                authorities.add(new SimpleGrantedAuthority(p));
            }
        }
        return authorities;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return identifier;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return enabled && status != io.github.devang559.authkit.lifecycle.AccountStatus.LOCKED
                && status != io.github.devang559.authkit.lifecycle.AccountStatus.DELETED;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return enabled && status != io.github.devang559.authkit.lifecycle.AccountStatus.DISABLED
                && status != io.github.devang559.authkit.lifecycle.AccountStatus.DELETED;
    }
}
