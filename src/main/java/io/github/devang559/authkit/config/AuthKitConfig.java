package io.github.devang559.authkit.config;

import io.github.devang559.authkit.exception.AuthKitConfigurationException;
import io.github.devang559.authkit.exception.RoleConfigurationException;
import io.github.devang559.authkit.exception.SchemaMismatchException;
import io.github.devang559.authkit.lifecycle.AccountStatus;
import io.github.devang559.authkit.user.UserField;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

@Getter
@ToString
@EqualsAndHashCode
public final class AuthKitConfig {

    private final AuthKitProperties properties;
    private final Set<UserField> enabledFields;
    private final Set<String> allowedRoles;
    private final boolean identifierEnabled;

    private AuthKitConfig(AuthKitProperties properties) {
        this.properties = properties;
        this.enabledFields = resolveEnabledFields(properties);
        this.allowedRoles = properties.getRoles().getAllowed() == null
                ? Collections.emptySet()
                : new HashSet<>(properties.getRoles().getAllowed());
        this.identifierEnabled = isFieldEnabled(toUserField(properties.getAuthentication().getIdentifier()));
    }

    public static AuthKitConfig of(AuthKitProperties properties) {
        AuthKitConfig config = new AuthKitConfig(properties);
        config.validate();
        return config;
    }

    private static Set<UserField> resolveEnabledFields(AuthKitProperties p) {
        Map<UserField, AuthKitProperties.UserFieldConfig> map = fieldMap(p);
        Set<UserField> result = new HashSet<>();
        for (UserField f : UserField.values()) {
            if (map.get(f).isEnabled()) {
                result.add(f);
            }
        }
        return Collections.unmodifiableSet(result);
    }

    private static Map<UserField, AuthKitProperties.UserFieldConfig> fieldMap(AuthKitProperties p) {
        Map<UserField, AuthKitProperties.UserFieldConfig> m = new EnumMap<>(UserField.class);
        m.put(UserField.USERNAME, p.getUser().getUsername());
        m.put(UserField.EMAIL, p.getUser().getEmail());
        m.put(UserField.PHONE, p.getUser().getPhone());
        m.put(UserField.FIRST_NAME, p.getUser().getFirstName());
        m.put(UserField.LAST_NAME, p.getUser().getLastName());
        m.put(UserField.DISPLAY_NAME, p.getUser().getDisplayName());
        m.put(UserField.AVATAR, p.getUser().getAvatar());
        return m;
    }

    public boolean isFieldEnabled(UserField field) {
        return enabledFields.contains(field);
    }

    public boolean isFieldRequired(UserField field) {
        return fieldMap(properties).get(field).isRequired();
    }

    public IdentifierType getIdentifier() {
        return properties.getAuthentication().getIdentifier();
    }

    private static UserField toUserField(IdentifierType id) {
        return switch (id) {
            case EMAIL -> UserField.EMAIL;
            case USERNAME -> UserField.USERNAME;
            case PHONE -> UserField.PHONE;
        };
    }

    public boolean isJwtEnabled() {
        return properties.getJwt().isEnabled();
    }

    public boolean isOtpEnabled() {
        return properties.getEmail().isOtpEnabled();
    }

    public boolean isEmailVerificationRequired() {
        return properties.getEmail().isVerificationRequired();
    }

    public boolean isRolesEnabled() {
        return properties.getRoles().isEnabled();
    }

    public boolean isPermissionsEnabled() {
        return properties.getPermissions().isEnabled();
    }

    public boolean isRateLimitEnabled() {
        return properties.getRateLimit().isEnabled();
    }

    public boolean isAccountLockoutEnabled() {
        return properties.getSecurity().isAccountLockoutEnabled();
    }

    public AccountStatus initialAccountStatus() {
        if (isEmailVerificationRequired() && getIdentifier() == IdentifierType.EMAIL) {
            return AccountStatus.PENDING_VERIFICATION;
        }
        if (isEmailVerificationRequired() && isFieldEnabled(UserField.EMAIL)) {
            return AccountStatus.PENDING_VERIFICATION;
        }
        return AccountStatus.ACTIVE;
    }

    private void validate() {
        if (!properties.isEnabled()) {
            return;
        }
        if (properties.getJwt().isEnabled()) {
            String secret = properties.getJwt().getSecret();
            if (secret == null || secret.isBlank()) {
                throw new AuthKitConfigurationException(
                        "spring.authkit.jwt.secret must be set when JWT is enabled. " +
                                "Provide a strong secret via configuration or the AUTHKIT_JWT_SECRET environment variable.");
            }
            if (secret.length() < 32) {
                throw new AuthKitConfigurationException(
                        "spring.authkit.jwt.secret is too weak: must be at least 32 characters.");
            }
        }

        if (!identifierEnabled) {
            throw new AuthKitConfigurationException(
                    "No authentication identifier has been configured. " +
                            "Enable at least one valid authentication identifier (email, username or phone).");
        }

        if (isEmailVerificationRequired() && !isFieldEnabled(UserField.EMAIL)) {
            throw new AuthKitConfigurationException(
                    "Email verification is required but the email user field is not enabled. " +
                            "Enable spring.authkit.user.email.enabled=true or disable email verification.");
        }

        if (isOtpEnabled() && !isFieldEnabled(UserField.EMAIL)) {
            throw new AuthKitConfigurationException(
                    "OTP is enabled but the email user field is not enabled, so an OTP destination cannot be resolved.");
        }

        if (properties.getEmail().getMaxAttempts() <= 0) {
            throw new AuthKitConfigurationException(
                    "spring.authkit.email.max-attempts must be a positive integer.");
        }
        if (properties.getEmail().getOtpLength() < 4 || properties.getEmail().getOtpLength() > 12) {
            throw new AuthKitConfigurationException(
                    "spring.authkit.email.otp-length must be between 4 and 12.");
        }

        if (properties.getPassword().getMinLength() < 8) {
            throw new AuthKitConfigurationException(
                    "spring.authkit.password.min-length must be at least 8.");
        }

        if (isRolesEnabled()) {
            String def = properties.getRoles().getDefaultRole();
            if (def == null || def.isBlank()) {
                throw new RoleConfigurationException(
                        "spring.authkit.roles.default-role must be set when roles are enabled.");
            }
            if (!allowedRoles.isEmpty() && !allowedRoles.contains(def)) {
                throw new RoleConfigurationException(
                        "Default role '" + def + "' is not in the configured allowed roles: " + allowedRoles + ".");
            }
        }
    }
}
