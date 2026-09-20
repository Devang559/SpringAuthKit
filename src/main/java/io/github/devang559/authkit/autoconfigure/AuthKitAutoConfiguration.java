package io.github.devang559.authkit.autoconfigure;

import io.github.devang559.authkit.authentication.AuthenticationService;
import io.github.devang559.authkit.authentication.EmailVerificationService;
import io.github.devang559.authkit.authentication.PasswordResetService;
import io.github.devang559.authkit.authentication.RegistrationService;
import io.github.devang559.authkit.config.AuthKitConfig;
import io.github.devang559.authkit.config.AuthKitProperties;
import io.github.devang559.authkit.email.DefaultEmailService;
import io.github.devang559.authkit.email.EmailService;
import io.github.devang559.authkit.jwt.JwtService;
import io.github.devang559.authkit.otp.OtpGenerator;
import io.github.devang559.authkit.otp.OtpRepository;
import io.github.devang559.authkit.otp.OtpService;
import io.github.devang559.authkit.otp.SecureRandomOtpGenerator;
import io.github.devang559.authkit.password.PasswordService;
import io.github.devang559.authkit.permission.AuthPermissionRepository;
import io.github.devang559.authkit.permission.PermissionService;
import io.github.devang559.authkit.ratelimit.InMemoryRateLimiter;
import io.github.devang559.authkit.ratelimit.RateLimiter;
import io.github.devang559.authkit.role.AuthRoleRepository;
import io.github.devang559.authkit.role.RoleService;
import io.github.devang559.authkit.token.RefreshTokenRepository;
import io.github.devang559.authkit.token.TokenService;
import io.github.devang559.authkit.user.AuthKitUserDetailsService;
import io.github.devang559.authkit.user.AuthUserService;
import io.github.devang559.authkit.user.AuthUserRepository;
import io.github.devang559.authkit.web.AuthKitErrorHandler;
import io.github.devang559.authkit.web.controller.AuthKitController;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Slf4j
@AutoConfiguration
@ConditionalOnClass({LocalContainerEntityManagerFactoryBean.class, AuthUserService.class})
@ConditionalOnProperty(name = "spring.authkit.enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(AuthKitProperties.class)
@EnableJpaRepositories(basePackages = "io.github.devang559.authkit")
@EntityScan(basePackages = "io.github.devang559.authkit")
public class AuthKitAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    AuthKitConfig authKitConfig(AuthKitProperties properties) {
        log.info("SpringAuthKit: constructing runtime configuration from 'spring.authkit' properties");
        return AuthKitConfig.of(properties);
    }

    @Bean
    @ConditionalOnMissingBean
    PasswordEncoder authKitPasswordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    @ConditionalOnMissingBean
    EmailService authKitEmailService(AuthKitConfig config) {
        return new DefaultEmailService(config);
    }

    @Bean
    @ConditionalOnMissingBean
    RateLimiter authKitRateLimiter() {
        return new InMemoryRateLimiter();
    }

    @Bean
    @ConditionalOnMissingBean
    OtpGenerator authKitOtpGenerator() {
        return new SecureRandomOtpGenerator();
    }

    @Bean
    @ConditionalOnMissingBean
    AuthUserService authUserService(AuthUserRepository repository, AuthKitConfig config) {
        return new AuthUserService(repository, config);
    }

    @Bean
    @ConditionalOnMissingBean
    JwtService jwtService(AuthKitConfig config) {
        return new JwtService(config);
    }

    @Bean
    @ConditionalOnMissingBean
    RoleService roleService(AuthRoleRepository repository, AuthKitConfig config) {
        return new RoleService(repository, config);
    }

    @Bean
    @ConditionalOnMissingBean
    PermissionService permissionService(AuthPermissionRepository repository) {
        return new PermissionService(repository);
    }

    @Bean
    @ConditionalOnMissingBean
    AuthKitUserDetailsService authKitUserDetailsService(
            AuthUserService userService, AuthKitConfig config, PermissionService permissionService) {
        return new AuthKitUserDetailsService(userService, config, permissionService);
    }

    @Bean
    @ConditionalOnMissingBean
    PasswordService passwordService(PasswordEncoder passwordEncoder, AuthKitConfig config) {
        return new PasswordService(passwordEncoder, config);
    }

    @Bean
    @ConditionalOnMissingBean
    OtpService otpService(AuthUserService userService, OtpRepository repository,
                          OtpGenerator generator, EmailService emailService,
                          RateLimiter rateLimiter, AuthKitConfig config) {
        return new OtpService(userService, repository, generator, emailService, rateLimiter, config);
    }

    @Bean
    @ConditionalOnMissingBean
    TokenService tokenService(JwtService jwtService, AuthUserService userService,
                              AuthKitUserDetailsService userDetailsService, AuthKitConfig config,
                              RoleService roleService, PermissionService permissionService,
                              RefreshTokenRepository repository) {
        return new TokenService(jwtService, userService, userDetailsService, config, roleService, permissionService, repository);
    }

    @Bean
    @ConditionalOnMissingBean
    RegistrationService registrationService(AuthUserService userService, PasswordService passwordService,
                                            RoleService roleService, OtpService otpService, AuthKitConfig config) {
        return new RegistrationService(userService, passwordService, roleService, otpService, config);
    }

    @Bean
    @ConditionalOnMissingBean
    AuthenticationService authenticationService(AuthUserService userService, TokenService tokenService,
                                                PasswordService passwordService, AuthKitConfig config) {
        return new AuthenticationService(userService, tokenService, passwordService, config);
    }

    @Bean
    @ConditionalOnMissingBean
    EmailVerificationService emailVerificationService(AuthUserService userService, OtpService otpService, AuthKitConfig config) {
        return new EmailVerificationService(userService, otpService, config);
    }

    @Bean
    @ConditionalOnMissingBean
    PasswordResetService passwordResetService(AuthUserService userService, OtpService otpService, PasswordService passwordService) {
        return new PasswordResetService(userService, otpService, passwordService);
    }

    @Bean
    @ConditionalOnMissingBean
    AuthKitController authKitController(RegistrationService registrationService, AuthenticationService authenticationService,
                                        EmailVerificationService emailVerificationService, PasswordResetService passwordResetService,
                                        OtpService otpService) {
        return new AuthKitController(registrationService, authenticationService, emailVerificationService, passwordResetService, otpService);
    }

    @Bean
    @ConditionalOnMissingBean
    AuthKitErrorHandler authKitErrorHandler() {
        return new AuthKitErrorHandler();
    }
}
