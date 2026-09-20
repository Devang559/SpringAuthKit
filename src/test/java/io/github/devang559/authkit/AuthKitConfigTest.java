package io.github.devang559.authkit;

import io.github.devang559.authkit.config.AuthKitConfig;
import io.github.devang559.authkit.config.AuthKitProperties;
import io.github.devang559.authkit.exception.AuthKitConfigurationException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AuthKitConfigTest {

    private AuthKitProperties props() {
        AuthKitProperties p = new AuthKitProperties();
        p.getJwt().setSecret("test-secret-very-long-and-secure-0123456789");
        return p;
    }

    @Test
    void validConfigBuilds() {
        AuthKitConfig config = AuthKitConfig.of(props());
        assertThat(config.getIdentifier().name()).isEqualTo("EMAIL");
        assertThat(config.isEmailVerificationRequired()).isTrue();
    }

    @Test
    void missingJwtSecretThrows() {
        AuthKitProperties p = new AuthKitProperties();
        p.getJwt().setSecret("");
        assertThatThrownBy(() -> AuthKitConfig.of(p))
                .isInstanceOf(AuthKitConfigurationException.class)
                .hasMessageContaining("jwt.secret");
    }

    @Test
    void weakJwtSecretThrows() {
        AuthKitProperties p = new AuthKitProperties();
        p.getJwt().setSecret("short");
        assertThatThrownBy(() -> AuthKitConfig.of(p))
                .isInstanceOf(AuthKitConfigurationException.class)
                .hasMessageContaining("too weak");
    }

    @Test
    void passwordPolicyTooShortThrows() {
        AuthKitProperties p = props();
        p.getPassword().setMinLength(4);
        assertThatThrownBy(() -> AuthKitConfig.of(p))
                .isInstanceOf(AuthKitConfigurationException.class)
                .hasMessageContaining("min-length");
    }

    @Test
    void emailVerificationWithoutEmailFieldThrows() {
        AuthKitProperties p = props();
        p.getUser().getEmail().setEnabled(false);
        assertThatThrownBy(() -> AuthKitConfig.of(p))
                .isInstanceOf(AuthKitConfigurationException.class)
                .hasMessageContaining("email");
    }
}
