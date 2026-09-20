package io.github.devang559.authkit.permission;

import io.github.devang559.authkit.role.AuthRole;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PermissionService {

    private final AuthPermissionRepository permissionRepository;

    @Transactional
    public AuthPermission ensurePermission(String name) {
        return permissionRepository.findByName(name)
                .orElseGet(() -> permissionRepository.save(new AuthPermission(name)));
    }

    @Transactional
    public AuthPermission assignPermission(AuthRole role, String permissionName) {
        AuthPermission permission = ensurePermission(permissionName);
        if (!role.getPermissions().contains(permission)) {
            role.getPermissions().add(permission);
        }
        return permission;
    }

    public Set<String> getPermissionNames(AuthRole role) {
        if (role.getPermissions() == null) {
            return new HashSet<>();
        }
        return role.getPermissions().stream()
                .map(AuthPermission::getName)
                .collect(Collectors.toSet());
    }
}
