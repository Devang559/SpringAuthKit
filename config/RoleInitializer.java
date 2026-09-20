package io.github.devang559.authkit.config;

import io.github.devang559.authkit.config.AuthKitConfig;
import io.github.devang559.authkit.role.RoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "spring.authkit.enabled", havingValue = "true", matchIfMissing = true)
public class RoleInitializer implements CommandLineRunner {

    private final AuthKitConfig config;
    private final RoleService roleService;

    @Override
    public void run(String... args) {
        if (config.isRolesEnabled()) {
            roleService.initializeRoles();
        }
    }
}
