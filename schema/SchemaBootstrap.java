package io.github.devang559.authkit.schema;

import io.github.devang559.authkit.config.AuthKitConfig;
import io.github.devang559.authkit.config.SchemaMode;
import io.github.devang559.authkit.exception.MigrationRequiredException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "spring.authkit.enabled", havingValue = "true", matchIfMissing = true)
@Order(100)
public class SchemaBootstrap implements CommandLineRunner {

    private final AuthKitConfig config;
    private final AuthKitSchemaManager schemaManager;

    @Override
    public void run(String... args) {
        if (!config.getProperties().isEnabled()) {
            return;
        }
        try {
            schemaManager.validate();
            if (config.getProperties().getSchema().getMode() == SchemaMode.EXTERNAL) {
                log.info("AuthKit schema validation (external mode) passed.");
            }
        } catch (MigrationRequiredException e) {
            throw new IllegalStateException("AuthKit schema migration is required: " + e.getMessage(), e);
        }
    }
}
