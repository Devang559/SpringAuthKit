package io.github.devang559.authkit.role;

import io.github.devang559.authkit.config.AuthKitConfig;
import io.github.devang559.authkit.exception.RoleConfigurationException;
import io.github.devang559.authkit.user.AuthUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RoleService {

    private final AuthRoleRepository roleRepository;
    private final AuthKitConfig config;

    @Transactional
    public void initializeRoles() {
        if (!config.isRolesEnabled()) {
            return;
        }
        for (String name : config.getProperties().getRoles().getAllowed()) {
            roleRepository.findByName(name).orElseGet(() -> roleRepository.save(new AuthRole(name)));
        }
    }

    @Transactional
    public AuthRole ensureRole(String name) {
        if (config.isRolesEnabled() && !config.getAllowedRoles().contains(name)) {
            throw new RoleConfigurationException(
                    "Role '" + name + "' is not in the configured allowed roles: " + config.getAllowedRoles() + ".");
        }
        return roleRepository.findByName(name)
                .orElseGet(() -> roleRepository.save(new AuthRole(name)));
    }

    @Transactional
    public void assignDefaultRole(AuthUser user) {
        if (!config.isRolesEnabled()) {
            return;
        }
        AuthRole role = ensureRole(config.getProperties().getRoles().getDefaultRole());
        user.addRole(role);
    }

    public Set<String> getRoleNames(AuthUser user) {
        if (user.getRoles() == null) {
            return new HashSet<>();
        }
        return user.getRoles().stream()
                .map(AuthRole::getName)
                .collect(Collectors.toSet());
    }

    public boolean hasRole(AuthUser user, String roleName) {
        return getRoleNames(user).contains(roleName);
    }
}
