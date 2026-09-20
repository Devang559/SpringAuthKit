package io.github.devang559.authkit.config;

import io.github.devang559.authkit.authentication.AuthenticationIdentifierResolver;
import io.github.devang559.authkit.authentication.DefaultAuthenticationIdentifierResolver;
import io.github.devang559.authkit.email.DefaultEmailService;
import io.github.devang559.authkit.email.EmailService;
import io.github.devang559.authkit.otp.OtpGenerator;
import io.github.devang559.authkit.ratelimit.InMemoryRateLimiter;
import io.github.devang559.authkit.ratelimit.RateLimiter;
import io.github.devang559.authkit.user.AuthUser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.*;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Slf4j
@Configuration(proxyBeanMethods = true)
@EnableConfigurationProperties(AuthKitProperties.class)
@ConditionalOnClass({AuthUser.class, LocalContainerEntityManagerFactoryBean.class})
@ConditionalOnProperty(name = "spring.authkit.enabled", havingValue = "true", matchIfMissing = true)
@ComponentScan(basePackages = "io.github.devang559.authkit",
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE,
                classes = {AuthKitAutoConfiguration.class, AuthKitSecurityAutoConfiguration.class}))
@EntityScan(basePackages = "io.github.devang559.authkit")
@EnableJpaRepositories(basePackages = "io.github.devang559.authkit")
public class AuthKitAutoConfiguration {

    @Bean
    public AuthKitConfig authKitConfig(AuthKitProperties properties) {
        AuthKitConfig config = AuthKitConfig.of(properties);
        log.info("SpringAuthKit auto-configuration enabled");
        return config;
    }

    @Bean
    @ConditionalOnMissingBean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    @ConditionalOnMissingBean
    public EmailService emailService(AuthKitConfig config) {
        return new DefaultEmailService(config);
    }

    @Bean
    @ConditionalOnMissingBean
    public OtpGenerator otpGenerator() {
        return new SecureRandomOtpGenerator();
    }

    @Bean
    @ConditionalOnMissingBean
    public RateLimiter rateLimiter() {
        return new InMemoryRateLimiter();
    }

    @Bean
    @ConditionalOnMissingBean
    public AuthenticationIdentifierResolver authenticationIdentifierResolver(AuthKitConfig config) {
        return new DefaultAuthenticationIdentifierResolver(config);
    }
}
