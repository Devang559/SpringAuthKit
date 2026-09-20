package io.github.devang559.authkit.support;

import io.github.devang559.authkit.email.EmailService;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

@TestConfiguration
public class TestEmailConfig {

    @Bean
    CapturingEmailService authKitTestEmailService() {
        return new CapturingEmailService();
    }
}
