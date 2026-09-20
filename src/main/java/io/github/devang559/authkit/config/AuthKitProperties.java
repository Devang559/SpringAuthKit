package io.github.devang559.authkit.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Getter
@Setter
@ConfigurationProperties(prefix = "spring.authkit")
public class AuthKitProperties {

    private boolean enabled = true;

    private Authentication authentication = new Authentication();

    private Schema schema = new Schema();

    private User user = new User();

    private Password password = new Password();

    private Jwt jwt = new Jwt();

    private Email email = new Email();

    private Roles roles = new Roles();

    private Permissions permissions = new Permissions();

    private Security security = new Security();

    private RateLimit rateLimit = new RateLimit();

    @Getter
    @Setter
    public static class Authentication {
        private IdentifierType identifier = IdentifierType.EMAIL;
    }

    @Getter
    @Setter
    public static class Schema {
        private SchemaMode mode = SchemaMode.MANAGED;
    }

    @Getter
    @Setter
    public static class User {
        private UserMode mode = UserMode.AUTHKIT;
        private UserFieldConfig username = new UserFieldConfig(false, false);
        private UserFieldConfig email = new UserFieldConfig(true, false);
        private UserFieldConfig phone = new UserFieldConfig(false, false);
        private UserFieldConfig firstName = new UserFieldConfig(false, false);
        private UserFieldConfig lastName = new UserFieldConfig(false, false);
        private UserFieldConfig displayName = new UserFieldConfig(false, false);
        private UserFieldConfig avatar = new UserFieldConfig(false, false);
    }

    @Getter
    @Setter
    public static class UserFieldConfig {
        private boolean enabled;
        private boolean required;

        public UserFieldConfig() {
        }

        public UserFieldConfig(boolean enabled, boolean required) {
            this.enabled = enabled;
            this.required = required;
        }
    }

    @Getter
    @Setter
    public static class Password {
        private int minLength = 8;
        private boolean requireUppercase = false;
        private boolean requireLowercase = true;
        private boolean requireNumber = true;
        private boolean requireSpecialCharacter = false;
    }

    @Getter
    @Setter
    public static class Jwt {
        private boolean enabled = true;
        private String secret = "";
        private Duration accessTokenExpiration = Duration.ofMinutes(15);
        private Duration refreshTokenExpiration = Duration.ofDays(7);
        private String issuer = "spring-authkit";
    }

    @Getter
    @Setter
    public static class Email {
        private boolean verificationRequired = true;
        private boolean otpEnabled = true;
        private int otpLength = 6;
        private Duration otpExpiration = Duration.ofMinutes(5);
        private int maxAttempts = 5;
        private String sender = "no-reply@authkit.local";
    }

    @Getter
    @Setter
    public static class Roles {
        private boolean enabled = true;
        private String defaultRole = "USER";
        @NestedConfigurationProperty
        private List<String> allowed = new ArrayList<>(Arrays.asList("USER", "ADMIN", "MODERATOR"));
    }

    @Getter
    @Setter
    public static class Permissions {
        private boolean enabled = true;
    }

    @Getter
    @Setter
    public static class Security {
        private boolean accountLockoutEnabled = true;
        private int maxFailedAttempts = 5;
        private Duration lockoutDuration = Duration.ofMinutes(15);
        @NestedConfigurationProperty
        private List<String> publicEndpoints = new ArrayList<>(Arrays.asList(
                "/auth/register",
                "/auth/login",
                "/auth/send-otp",
                "/auth/verify-otp",
                "/auth/resend-otp",
                "/auth/forgot-password",
                "/auth/reset-password",
                "/auth/refresh",
                "/auth/logout",
                "/v3/api-docs/**",
                "/swagger-ui/**",
                "/webjars/**"
        ));
    }

    @Getter
    @Setter
    public static class RateLimit {
        private boolean enabled = true;
        private Policy login = new Policy(5, Duration.ofMinutes(1));
        private Policy otp = new Policy(5, Duration.ofMinutes(5));

        @Getter
        @Setter
        public static class Policy {
            private int maxAttempts;
            private Duration window;

            public Policy() {
            }

            public Policy(int maxAttempts, Duration window) {
                this.maxAttempts = maxAttempts;
                this.window = window;
            }
        }
    }
}
